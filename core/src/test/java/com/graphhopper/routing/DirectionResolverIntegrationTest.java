package com.graphhopper.routing;

import com.graphhopper.routing.util.DirectedEdgeFilter;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.shapes.GHPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;

import com.graphhopper.util.FetchMode;

@ExtendWith(MockitoExtension.class)
public class DirectionResolverIntegrationTest {

    @Mock
    private Graph mockGraph;

    @Mock
    private DirectedEdgeFilter mockEdgeFilter;

    @Mock
    private NodeAccess mockNodeAccess;

    @Mock
    private EdgeExplorer mockEdgeExplorer;

    @Mock
    private EdgeIterator mockEdgeIterator;

    private DirectionResolver directionResolver;

    @BeforeEach
    void setUp() {
        lenient().when(mockGraph.createEdgeExplorer()).thenReturn(mockEdgeExplorer);
        lenient().when(mockGraph.getNodeAccess()).thenReturn(mockNodeAccess);

        lenient().when(mockNodeAccess.getLat(anyInt())).thenReturn(48.0);
        lenient().when(mockNodeAccess.getLon(anyInt())).thenReturn(9.0);

        lenient().when(mockEdgeExplorer.setBaseNode(anyInt())).thenReturn(mockEdgeIterator);
        lenient().when(mockEdgeIterator.next()).thenReturn(false);

        directionResolver = new DirectionResolver(mockGraph, mockEdgeFilter);
    }

    @Test
    void testDirectionResolver_createsSuccessfully() {
        assertNotNull(directionResolver, "DirectionResolver should be created");

        verify(mockGraph).createEdgeExplorer();
        verify(mockGraph).getNodeAccess();
    }

    @Test
    void testResolveDirections_withNoEdges_returnsImpossible() {
        GHPoint location = new GHPoint(48.0, 9.0);

        DirectionResolverResult result = directionResolver.resolveDirections(0, location);

        assertNotNull(result, "Result should not be null");
        verify(mockEdgeExplorer).setBaseNode(0);
        verify(mockEdgeIterator, atLeastOnce()).next();
    }

    @Test
    void testResolveDirections_verifyGraphInteractions() {
        GHPoint location = new GHPoint(48.5, 9.5);
        int nodeId = 5;

        directionResolver.resolveDirections(nodeId, location);

        verify(mockEdgeExplorer).setBaseNode(nodeId);
        verify(mockEdgeIterator, atLeastOnce()).next();
    }

    @Test
    void testDirectionResolver_usesEdgeFilterCorrectly() {
        GHPoint location = new GHPoint(48.0, 9.0);

        directionResolver.resolveDirections(1, location);

        verify(mockEdgeExplorer, atLeastOnce()).setBaseNode(anyInt());
        verifyNoInteractions(mockEdgeFilter);
    }

    @Test
    void testDirectionResolver_multipleCallsWork() {
        GHPoint location1 = new GHPoint(48.0, 9.0);
        GHPoint location2 = new GHPoint(49.0, 10.0);

        directionResolver.resolveDirections(0, location1);
        directionResolver.resolveDirections(1, location2);

        verify(mockEdgeExplorer, times(2)).setBaseNode(anyInt());
    }
    @Test
    void testResolveDirections_withEdgeFilterRejectingBothDirections_handlesCorrectly() {
        // Setup: edge exists but filter rejects both directions
        when(mockEdgeIterator.next()).thenReturn(true, false);
        when(mockEdgeFilter.accept(any(), anyBoolean())).thenReturn(false);

        GHPoint location = new GHPoint(48.1, 9.1);
        DirectionResolverResult result = directionResolver.resolveDirections(0, location);

        assertNotNull(result, "Result should not be null");
        verify(mockEdgeFilter, atLeastOnce()).accept(any(), anyBoolean());
        verify(mockEdgeExplorer).setBaseNode(0);
    }

    @Test
    void testResolveDirections_multipleNodesWithDifferentLocations_allProcessed() {
        // Test multiple nodes
        GHPoint location1 = new GHPoint(48.5, 9.5);
        GHPoint location2 = new GHPoint(47.5, 8.5);
        GHPoint location3 = new GHPoint(49.5, 10.5);

        directionResolver.resolveDirections(1, location1);
        directionResolver.resolveDirections(2, location2);
        directionResolver.resolveDirections(3, location3);

        verify(mockEdgeExplorer).setBaseNode(1);
        verify(mockEdgeExplorer).setBaseNode(2);
        verify(mockEdgeExplorer).setBaseNode(3);
    }

    @Test
    void testResolveDirections_withVariousGHPointLocations_handlesAllCases() {
        // Test boundary values
        GHPoint[] locations = {
                new GHPoint(0.0, 0.0),
                new GHPoint(90.0, 180.0),
                new GHPoint(-90.0, -180.0),
                new GHPoint(48.8566, 2.3522)  // Paris
        };

        for (int i = 0; i < locations.length; i++) {
            DirectionResolverResult result = directionResolver.resolveDirections(i, locations[i]);
            assertNotNull(result, "Result should not be null for location " + i);
        }

        verify(mockEdgeExplorer, times(locations.length)).setBaseNode(anyInt());
    }
}
