package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

    @Test
    public void testWhenResolveExistingLocationShouldReturn() {
        // given
        LocationGateway locationGateway = new LocationGateway();

        // when
        Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

        // then
        assertNotNull(location);
        assertEquals("ZWOLLE-001", location.identification);
        assertEquals(1, location.maxNumberOfWarehouses);
        assertEquals(40, location.maxCapacity);
    }

    @Test
    public void testWhenResolveNonExistingLocationShouldReturnNull() {
        // given
        LocationGateway locationGateway = new LocationGateway();

        // when
        Location location = locationGateway.resolveByIdentifier("NON-EXISTENT");

        // then
        assertNull(location);
    }

    @Test
    public void testWhenResolveAmsterdamLocationShouldReturn() {
        // given
        LocationGateway locationGateway = new LocationGateway();

        // when
        Location location = locationGateway.resolveByIdentifier("AMSTERDAM-001");

        // then
        assertNotNull(location);
        assertEquals("AMSTERDAM-001", location.identification);
        assertEquals(5, location.maxNumberOfWarehouses);
        assertEquals(100, location.maxCapacity);
    }
}
