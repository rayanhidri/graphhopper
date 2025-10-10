package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.*;
import com.graphhopper.storage.IntsRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OSMTollParserTest {
    private EnumEncodedValue<Toll> tollEnc;
    private OSMTollParser parser;

    @BeforeEach
    public void setUp() {
        tollEnc = Toll.create();
        tollEnc.init(new EncodedValue.InitializerConfig());
        parser = new OSMTollParser(tollEnc);
    }

    @Test
    public void testSimpleTags() {
        ReaderWay readerWay = new ReaderWay(1);
        IntsRef relFlags = new IntsRef(2);
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        readerWay.setTag("highway", "primary");
        parser.handleWayTags(edgeId, edgeIntAccess, readerWay, relFlags);
        assertEquals(Toll.NO, tollEnc.getEnum(false, edgeId, edgeIntAccess));

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        readerWay.setTag("highway", "primary");
        readerWay.setTag("toll:hgv", "yes");
        parser.handleWayTags(edgeId, edgeIntAccess, readerWay, relFlags);
        assertEquals(Toll.HGV, tollEnc.getEnum(false, edgeId, edgeIntAccess));

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        readerWay.setTag("highway", "primary");
        readerWay.setTag("toll:N2", "yes");
        parser.handleWayTags(edgeId, edgeIntAccess, readerWay, relFlags);
        assertEquals(Toll.HGV, tollEnc.getEnum(false, edgeId, edgeIntAccess));

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        readerWay.setTag("highway", "primary");
        readerWay.setTag("toll:N3", "yes");
        parser.handleWayTags(edgeId, edgeIntAccess, readerWay, relFlags);
        assertEquals(Toll.HGV, tollEnc.getEnum(false, edgeId, edgeIntAccess));

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        readerWay.setTag("highway", "primary");
        readerWay.setTag("toll", "yes");
        parser.handleWayTags(edgeId, edgeIntAccess, readerWay, relFlags);
        assertEquals(Toll.ALL, tollEnc.getEnum(false, edgeId, edgeIntAccess));

        edgeIntAccess = new ArrayEdgeIntAccess(1);
        readerWay.setTag("highway", "primary");
        readerWay.setTag("toll", "yes");
        readerWay.setTag("toll:hgv", "yes");
        readerWay.setTag("toll:N2", "yes");
        readerWay.setTag("toll:N3", "yes");
        parser.handleWayTags(edgeId, edgeIntAccess, readerWay, relFlags);
        assertEquals(Toll.ALL, tollEnc.getEnum(false, edgeId, edgeIntAccess));
    }

    @Test
    void country() {
        assertEquals(Toll.ALL, getToll("motorway", "", Country.HUN));
        assertEquals(Toll.HGV, getToll("trunk", "", Country.HUN));
        assertEquals(Toll.HGV, getToll("primary", "", Country.HUN));
        assertEquals(Toll.NO, getToll("secondary", "", Country.HUN));
        assertEquals(Toll.NO, getToll("tertiary", "", Country.HUN));

        assertEquals(Toll.ALL, getToll("motorway", "", Country.FRA));
        assertEquals(Toll.NO, getToll("trunk", "", Country.FRA));
        assertEquals(Toll.NO, getToll("primary", "", Country.FRA));

        assertEquals(Toll.NO, getToll("motorway", "", Country.MEX));
        assertEquals(Toll.NO, getToll("trunk", "", Country.MEX));
        assertEquals(Toll.NO, getToll("primary", "", Country.MEX));

        assertEquals(Toll.ALL, getToll("secondary", "toll=yes", Country.HUN));
        assertEquals(Toll.HGV, getToll("secondary", "toll:hgv=yes", Country.HUN));
        assertEquals(Toll.HGV, getToll("secondary", "toll:N3=yes", Country.HUN));
        assertEquals(Toll.NO, getToll("secondary", "toll=no", Country.HUN));
    }

    private Toll getToll(String highway, String toll, Country country) {
        ReaderWay readerWay = new ReaderWay(123L);
        readerWay.setTag("highway", highway);
        readerWay.setTag("country", country);
        String[] tollKV = toll.split("=");
        if (tollKV.length > 1)
            readerWay.setTag(tollKV[0], tollKV[1]);
        IntsRef relFlags = new IntsRef(2);
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, readerWay, relFlags);
        return tollEnc.getEnum(false, edgeId, edgeIntAccess);
    }

    /**
     * Test pour la Suisse (CHE) avec une autoroute (motorway).
     * Vérifie que les autoroutes en Suisse ont un péage pour tous les véhicules (Toll.ALL).
     */
    @Test
    void testSwitzerlandMotorwayToll() {
        assertEquals(Toll.ALL, getToll("motorway", "", Country.CHE));
    }

    /**
     * Test pour la Suisse (CHE) avec une route secondaire.
     * Vérifie que les routes non-autoroutes en Suisse ont un péage HGV uniquement (poids lourds).
     * En Suisse, il y a une 'Schwerlastabgabe' (taxe poids lourds) sur tout le réseau routier.
     */
    @Test
    void testSwitzerlandSecondaryRoadToll() {
        assertEquals(Toll.HGV, getToll("secondary", "", Country.CHE));
    }

    /**
     * Test pour l'Allemagne (DEU) avec une autoroute.
     * Vérifie que les autoroutes en Allemagne ont un péage HGV uniquement (poids lourds).
     */
    @Test
    void testGermanyMotorwayToll() {
        assertEquals(Toll.HGV, getToll("motorway", "", Country.DEU));
    }

    /**
     * Test pour la Roumanie (ROU) avec une route principale (trunk).
     * Vérifie que les routes trunk en Roumanie ont un péage pour tous les véhicules (Toll.ALL).
     */
    @Test
    void testRomaniaTrunkRoadToll() {
        assertEquals(Toll.ALL, getToll("trunk", "", Country.ROU));
    }
    @Test
    void tollNo_overridesCountryDefault() {
        // Mutant visé: L45 "removed conditional - replaced equality check with false" (SURVIVED)
        // Si "toll=no" est ignoré par le mutant, on tomberait dans le défaut pays (FRA+motorway -> ALL).
        // Le test attend NO, donc il tue le mutant.
        assertEquals(Toll.NO, getToll("motorway", "toll=no", Country.FRA));
    }

    @Test
    void switzerland_trunk_isAll() {
        // Mutant visé: L69 "removed conditional - replaced equality check with false" (SURVIVED)
        // On teste la 2e partie du '||' (TRUNK) pour CHE -> doit être ALL.
        assertEquals(Toll.ALL, getToll("trunk", "", Country.CHE));
    }
    /**
     * Test avec java-faker : génère des données de test aléatoires pour tester la robustesse.
     * Intention : Vérifier que le parser gère correctement des routes avec des noms de pays aléatoires.
     * Motivation : Utiliser java-faker pour créer des tests avec des données variées et imprévisibles.
     * Oracle : Les pays non reconnus doivent retourner Toll.NO (comportement par défaut).
     */
    @Test
    void testRandomCountryWithFaker() {
        com.github.javafaker.Faker faker = new com.github.javafaker.Faker();

        // Génère un nom de pays aléatoire
        String randomCountryName = faker.country().name();

        // Crée un ReaderWay avec un pays non reconnu (String au lieu de Country enum)
        ReaderWay readerWay = new ReaderWay(faker.number().randomNumber());
        readerWay.setTag("highway", faker.options().option("motorway", "trunk", "primary", "secondary"));
        // Note: on ne peut pas utiliser randomCountryName directement car Country est un enum
        // On teste avec MISSING qui représente un pays non reconnu
        readerWay.setTag("country", Country.MISSING);

        IntsRef relFlags = new IntsRef(2);
        EdgeIntAccess edgeIntAccess = new ArrayEdgeIntAccess(1);
        int edgeId = 0;
        parser.handleWayTags(edgeId, edgeIntAccess, readerWay, relFlags);

        // Pour un pays non reconnu, le comportement par défaut est Toll.NO
        assertEquals(Toll.NO, tollEnc.getEnum(false, edgeId, edgeIntAccess));
    }

}
