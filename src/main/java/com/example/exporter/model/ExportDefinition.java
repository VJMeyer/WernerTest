package com.example.exporter.model;

import java.util.List;
import java.util.Map;

/**
 * Defines an export configuration including the main table and lookup resolutions.
 *
 * @param name Name of this export definition
 * @param mainQuery The main SQL query for the large table (without JOINs)
 * @param columns List of column names to export (in order)
 * @param lookupResolutions Map of main table column -> lookup definition and output columns
 */
public record ExportDefinition(
        String name,
        String mainQuery,
        List<String> columns,
        Map<String, LookupResolution> lookupResolutions
) {

    /**
     * Defines how to resolve a foreign key to lookup values.
     *
     * @param lookupName The name of the lookup cache to use
     * @param outputColumns The columns to add to the output from the lookup
     */
    public record LookupResolution(
            String lookupName,
            List<String> outputColumns
    ) {
    }

    /**
     * Get all output columns including resolved lookups.
     */
    public List<String> getAllOutputColumns() {
        var result = new java.util.ArrayList<>(columns);

        // Add lookup columns after their respective foreign key columns
        for (var entry : lookupResolutions.entrySet()) {
            String fkColumn = entry.getKey();
            LookupResolution resolution = entry.getValue();

            int index = result.indexOf(fkColumn);
            if (index >= 0) {
                // Insert lookup columns right after the FK column
                result.addAll(index + 1, resolution.outputColumns());
            } else {
                // FK column not in output, just append lookup columns
                result.addAll(resolution.outputColumns());
            }
        }

        return result;
    }
}
