package com.worknest.domain.enums;

/**
 * Declares how the site's location data was originally captured during the setup flow.
 *
 * <p>This enum is informational only — it is stored on the site for auditing / UX purposes
 * and does not affect any server-side validation logic. The authoritative location data
 * are always the {@code latitude} / {@code longitude} / geofence fields on {@link
 * com.worknest.domain.entities.CompanySite}.
 *
 * <ul>
 *   <li>{@code BROWSER_GEOLOCATION} – coordinates were captured via the browser's
 *       {@code navigator.geolocation} API on the client device.</li>
 *   <li>{@code MANUAL_ENTRY} – the admin typed or adjusted the coordinates / address by hand.</li>
 *   <li>{@code MAP_PIN} – coordinates were selected by dragging a map pin in the UI.</li>
 * </ul>
 */
public enum LocationDetectionSource {

    /**
     * High-accuracy coordinates captured by {@code navigator.geolocation} on the user's device.
     */
    BROWSER_GEOLOCATION,

    /**
     * Coordinates were typed or edited manually by the admin after geocoding.
     */
    MANUAL_ENTRY,

    /**
     * Coordinates were selected by placing / dragging a map pin in the UI.
     */
    MAP_PIN
}
