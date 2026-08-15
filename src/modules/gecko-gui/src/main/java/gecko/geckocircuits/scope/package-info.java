/**
 * Legacy Fourier visualization cluster.
 *
 * <p>This package was originally targeted for full migration into
 * {@link gecko.geckocircuits.newscope} (see CLEANUP_TODO #2). The migration
 * stalled because the four "survivors" ({@link DialogFourierDiagram},
 * {@link FourierDiagram}, {@link FourierCurveReconstruction}, and the
 * supporting {@link GraferV3} / {@link GraferImplementation}) form a tightly
 * coupled cluster that depends on the {@code @Deprecated} {@link GraferV3}
 * stack rather than the modern {@link gecko.geckocircuits.newscope.GraferV4}.
 *
 * <p>What was done in cleanup #2:
 * <ul>
 *   <li>Deleted the truly-dead {@code FourierPlotFrame} (was only referenced
 *       inside an {@code if (0 > 1)} dead block in
 *       {@link gecko.geckocircuits.newscope.DialogFourier}).</li>
 *   <li>Removed the corresponding dead block and unused import.</li>
 * </ul>
 *
 * <p>What was NOT done, and why:
 * <ul>
 *   <li>The remaining ten classes are kept because they reference each other
 *       via same-package types ({@link DataContainer}, {@link DataContainerSimple},
 *       {@link DisplayFourierWorksheet}, {@link HiLoData}, {@link Scopable}).
 *       An earlier "external callers only" search mis-identified them as
 *       dead; they are not.</li>
 *   <li>Relocating the cluster into {@code newscope/} was considered and
 *       rejected as a 5000+ line mechanical rename of {@code @Deprecated}
 *       code that would not actually modernize anything.</li>
 *   <li>A proper migration requires rewriting the Fourier dialogs on top of
 *       {@link gecko.geckocircuits.newscope.GraferV4} and the modern
 *       scope-signal abstractions. That is a multi-day refactor out of scope
 *       for a dead-code cleanup pass.</li>
 * </ul>
 *
 * <p>Net result of cleanup #2: one dead class removed, the cross-package
 * leak from {@code newscope/DialogFourier.java} to the dead
 * {@code FourierPlotFrame} is gone. The remaining cross-package reference
 * ({@code newscope/DialogFourier.java} -> {@link DialogFourierDiagram}) is
 * load-bearing and intentional; it cannot be removed without the rewrite
 * described above.
 *
 * <p>The {@code @Deprecated} classes in this package ({@link DataContainer},
 * {@link DataContainerSimple}, {@link DisplayFourierWorksheet},
 * {@link GraferImplementation}, {@link GraferV3}, {@link HiLoData},
 * {@link Scopable}) are deprecation in the technical sense only - they are
 * load-bearing for the Fourier cluster and must not be deleted piecemeal.
 * Delete the cluster as a whole, after the GraferV4 rewrite, or leave it
 * alone.
 */
package gecko.geckocircuits.scope;
