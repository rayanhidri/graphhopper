package com.graphhopper.routing;

import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeIteratorState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PathExtractorIntegrationTest {

    @Mock
    private Graph mockGraph;

    @Mock
    private Weighting mockWeighting;

    @Mock
    private NodeAccess mockNodeAccess;

    @Mock
    private EdgeIteratorState mockEdgeState;

    private PathExtractor pathExtractor;

    @BeforeEach
    void setUp() {
        lenient().when(mockGraph.getNodeAccess()).thenReturn(mockNodeAccess);
        lenient().when(mockGraph.getEdges()).thenReturn(10);
        lenient().when(mockGraph.getEdgeIteratorState(anyInt(), anyInt())).thenReturn(mockEdgeState);

        lenient().when(mockEdgeState.getDistance()).thenReturn(100.0);
        lenient().when(mockEdgeState.getEdge()).thenReturn(0);

        pathExtractor = new PathExtractor(mockGraph, mockWeighting);
    }

    @Test
    void testExtractPath_withNullEntry_returnsNotFoundPath() {
        SPTEntry nullEntry = null;

        Path result = pathExtractor.extract(nullEntry);

        assertNotNull(result, "Path should not be null");
        assertFalse(result.isFound(), "Path should not be found for null entry");
    }

    @Test
    void testExtractPath_withSingleNode_returnsFoundPath() {
        SPTEntry sptEntry = new SPTEntry(0, 10.5);

        Path result = pathExtractor.extract(sptEntry);

        assertTrue(result.isFound(), "Path should be found");
        assertEquals(10.5, result.getWeight(), 0.01, "Weight should match SPTEntry weight");

        verify(mockGraph, atLeastOnce()).getNodeAccess();
    }

    @Test
    void testExtractPath_verifyGraphInteractions() {
        SPTEntry sptEntry = new SPTEntry(1, 8.5);

        Path result = pathExtractor.extract(sptEntry);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isFound(), "Path should be found");

        verify(mockGraph, atLeastOnce()).getNodeAccess();
    }

    @Test
    void testExtractPath_withMultipleNodesChain() {
        SPTEntry node0 = new SPTEntry(0, 0.0);
        SPTEntry node1 = new SPTEntry(0, 1, 5.0, node0);

        Path result = pathExtractor.extract(node1);

        assertTrue(result.isFound(), "Path should be found");
        assertEquals(5.0, result.getWeight(), 0.01, "Weight should be 5.0");

        verify(mockGraph, atLeastOnce()).getNodeAccess();
        verify(mockGraph, atLeastOnce()).getEdgeIteratorState(anyInt(), anyInt());
    }

    @Test
    void testPathExtractor_mocksWorkCorrectly() {
        SPTEntry sptEntry = new SPTEntry(2, 15.0);

        Path result = pathExtractor.extract(sptEntry);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isFound(), "Path should be found");
        assertEquals(15.0, result.getWeight(), 0.01, "Weight should be 15.0");

        verify(mockGraph, atLeastOnce()).getNodeAccess();
    }
}
