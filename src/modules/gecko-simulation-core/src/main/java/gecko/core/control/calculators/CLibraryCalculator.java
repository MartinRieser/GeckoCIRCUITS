/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.control.calculators;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * NativeC v2: control block (typ 88) that binds a user-built host shared
 * library implementing the {@code gecko_c_block.h} contract.
 *
 * <p>Firmware-in-the-loop: the user builds their microcontroller control code
 * with their own toolchain into a {@code .dll}/{@code .so}/{@code .dylib};
 * Gecko never compiles C. Required library symbol: {@code gecko_step};
 * optional: {@code gecko_init}, {@code gecko_deinit}.</p>
 *
 * <p>Loading uses the FFM API. The library file is copied to a unique temp
 * file per simulation run, so (a) the user's original file is never locked —
 * it can be rebuilt while the app is open — and (b) every run loads a fresh
 * library image, giving C/C++ static objects a power-on-reset semantics that
 * matches the real device.</p>
 *
 * <p>Failure contract (mirrors {@link ScriptBlockCalculator}): if the library
 * or a required symbol cannot be loaded, outputs hold at their initial values,
 * the error is logged once and available via {@link #getLoadError()}.</p>
 */
public final class CLibraryCalculator extends AbstractControlCalculatable
        implements InitializableAtSimulationStart {

    private static final Logger LOGGER = LogManager.getLogger(CLibraryCalculator.class);

    private final String libraryPath;
    private final Arena arena;
    private MemorySegment xSegment;
    private MemorySegment ySegment;
    private MethodHandle stepHandle;
    private Runnable initHook;
    private Runnable deinitHook;
    private String loadError;
    private boolean initCalled;

    public CLibraryCalculator(final int noInputs, final int noOutputs, final String libraryPath) {
        super(noInputs, noOutputs);
        this.libraryPath = libraryPath;
        this.arena = Arena.ofAuto();
    }

    /** Load error, or null when the library bound successfully. */
    public String getLoadError() {
        return loadError;
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        if (libraryPath == null || libraryPath.isBlank()) {
            loadError = "no library path configured";
            LOGGER.warn("NativeC block has no library path - outputs hold at initial value");
            return;
        }
        try {
            loadAndBind();
        } catch (Throwable t) {
            loadError = "cannot load '" + libraryPath + "': " + t.getMessage();
            LOGGER.warn("NativeC block load failed: {}", loadError);
        }
    }

    private void loadAndBind() throws Throwable {
        final Path source = Path.of(libraryPath);
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("library file not found: " + source.toAbsolutePath());
        }
        // unique per-run copy: user file stays unlocked, fresh C statics per run
        final String fileName = source.getFileName().toString();
        final String suffix = fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.')) : ".bin";
        final Path copy = Files.createTempFile("gecko-nativec-", suffix);
        Files.copy(source, copy, StandardCopyOption.REPLACE_EXISTING);
        copy.toFile().deleteOnExit();

        final SymbolLookup symbols = SymbolLookup.libraryLookup(copy, arena);
        final Linker linker = Linker.nativeLinker();

        final Optional<MemorySegment> step = symbols.find("gecko_step");
        if (step.isEmpty()) {
            throw new IllegalArgumentException("required symbol gecko_step not found");
        }
        stepHandle = linker.downcallHandle(step.get(), FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));

        initHook = optionalVoid(symbols, linker, "gecko_init");
        deinitHook = optionalVoid(symbols, linker, "gecko_deinit");

        xSegment = arena.allocate(ValueLayout.JAVA_DOUBLE,
                Math.max(1, _inputSignal.length));
        ySegment = arena.allocate(ValueLayout.JAVA_DOUBLE,
                Math.max(1, _outputSignal.length));

        if (initHook != null) {
            initHook.run();
        }
        initCalled = true;
    }

    private static Runnable optionalVoid(SymbolLookup symbols, Linker linker, String name) {
        final Optional<MemorySegment> symbol = symbols.find(name);
        if (symbol.isEmpty()) {
            return null;
        }
        final MethodHandle handle = linker.downcallHandle(symbol.get(), FunctionDescriptor.ofVoid());
        return () -> {
            try {
                handle.invokeExact();
            } catch (Throwable t) {
                LOGGER.warn("optional hook {} threw: {}", name, t.getMessage());
            }
        };
    }

    @Override
    public void calculateYOUT(final double deltaT) {
        if (loadError != null || stepHandle == null) {
            // failure contract: outputs hold at their initial values
            return;
        }
        final int nIn = _inputSignal.length;
        for (int i = 0; i < nIn; i++) {
            final double[] in = _inputSignal[i];
            xSegment.setAtIndex(ValueLayout.JAVA_DOUBLE, i,
                    in != null && in.length > 0 ? in[0] : 0.0);
        }
        try {
            stepHandle.invokeExact(xSegment, nIn, ySegment, _outputSignal.length,
                    AbstractControlCalculatable._time, deltaT);
        } catch (Throwable t) {
            LOGGER.warn("gecko_step invocation failed: {}", t.getMessage(), t);
            return;
        }
        for (int i = 0; i < _outputSignal.length; i++) {
            final double[] out = _outputSignal[i];
            if (out != null && out.length > 0) {
                out[0] = ySegment.getAtIndex(ValueLayout.JAVA_DOUBLE, i);
            }
        }
    }

    /** Releases the loaded library; safe to call multiple times. */
    public void close() {
        if (initCalled && deinitHook != null) {
            initCalled = false;
            try {
                deinitHook.run();
            } catch (Throwable t) {
                LOGGER.warn("gecko_deinit threw: {}", t.getMessage());
            }
        }
    }
}
