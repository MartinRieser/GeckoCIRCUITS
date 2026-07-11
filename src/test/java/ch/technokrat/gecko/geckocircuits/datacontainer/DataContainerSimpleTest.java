package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Test;
import static org.junit.Assert.*;

public class DataContainerSimpleTest {

    @Test
    public void testDataContainerOperations() {
        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(3, 10);
        assertEquals(3, container.getRowLength());

        // Set value in row 1, column 5 (note that setValue asserts row > 0)
        container.setValue(12.5f, 1, 5);
        assertEquals(12.5f, container.getValue(1, 5), 1e-6f);

        container.setValue(-3.5f, 2, 2);
        assertEquals(-3.5f, container.getValue(2, 2), 1e-6f);

        // Delete data reference
        container.deleteDataReference();
        try {
            container.getValue(0, 0);
            fail("Expected NullPointerException after deleting data reference");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testFabricArrayTimeSeries() {
        DataContainerSimple container = DataContainerSimple.fabricArrayTimeSeries(2, 5);
        assertEquals(2, container.getRowLength());
    }
}
