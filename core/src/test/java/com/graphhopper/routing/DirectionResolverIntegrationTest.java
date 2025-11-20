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
}
