/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with GeckoCIRCUITS.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko;

import javax.swing.JOptionPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Modal Swing dialogs deadlock RMI-driven headless runs (GeckoSim started by
 * the REST backend with {@code -Dgecko.headless=true}): nobody can click the
 * dialog away, so main blocks before the RMI server comes up. Headless runs
 * log the message instead.
 */
public final class HeadlessDialogs {

    private static final Logger LOGGER = LogManager.getLogger(HeadlessDialogs.class);

    private HeadlessDialogs() {
    }

    public static boolean isActive() {
        return Boolean.getBoolean("gecko.headless");
    }

    /**
     * Shows a modal parentless message dialog, or logs it when running with
     * {@code gecko.headless=true}.
     */
    public static void showMessageOrLog(String message, String title, int messageType) {
        if (isActive()) {
            LOGGER.warn("headless: suppressed dialog [{}] {}", title, message.replace("\n", " | "));
            return;
        }
        JOptionPane.showMessageDialog(null, message, title, messageType);
    }
}
