package com.example.magyarorszagkincsei.data;

import com.google.firebase.firestore.Exclude;
import java.util.ArrayList;
import java.util.List;

/**
 * Ez az osztály az alkalmazás adatmodellje (POJO).
 * Kezeli a régi (egy kép) és az új (több kép) adatstruktúrát is.
 */
public class Kincs {

    private String nev;
    private String leiras;
    private String felfedezo;
    private List<String> kepUrlLista;
    private double lat;
    private double lng;
    private float atlagErtekeles;
    private int ertekelesekSzama;

    @Exclude
    private String id;

    public Kincs() {
        this.kepUrlLista = new ArrayList<>();
    }

    public Kincs(String nev, String leiras, String felfedezo, List<String> kepUrlLista, double lat, double lng) {
        this.nev = nev;
        this.leiras = leiras;
        this.felfedezo = felfedezo;
        this.kepUrlLista = kepUrlLista != null ? kepUrlLista : new ArrayList<>();
        this.lat = lat;
        this.lng = lng;
        this.atlagErtekeles = 0;
        this.ertekelesekSzama = 0;
    }

    // --- KOMPATIBILITÁSI JAVÍTÁS A RÉGI ADATOKHOZ ---
    
    /**
     * Ezt a metódust a Firebase hívja meg, ha a régi "kepUrl" mezőt találja a dokumentumban.
     * Behelyezi a szöveges URL-t a listába, így az app mindenhol listaként kezeli.
     */
    public void setKepUrl(String kepUrl) {
        if (this.kepUrlLista == null) {
            this.kepUrlLista = new ArrayList<>();
        }
        if (kepUrl != null && !this.kepUrlLista.contains(kepUrl)) {
            this.kepUrlLista.add(kepUrl);
        }
    }

    /**
     * Visszaadja az első képet (index képnek), vagy null-t, ha nincs kép.
     */
    public String getKepUrl() {
        return (kepUrlLista != null && !kepUrlLista.isEmpty()) ? kepUrlLista.get(0) : null;
    }

    // --- GETTEREK ÉS SETTEREK ---

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getNev() { return nev; }
    public void setNev(String nev) { this.nev = nev; }

    public String getLeiras() { return leiras; }
    public void setLeiras(String leiras) { this.leiras = leiras; }

    public String getFelfedezo() { return felfedezo; }
    public void setFelfedezo(String felfedezo) { this.felfedezo = felfedezo; }

    public List<String> getKepUrlLista() { return kepUrlLista; }
    public void setKepUrlLista(List<String> kepUrlLista) { this.kepUrlLista = kepUrlLista; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public float getAtlagErtekeles() { return atlagErtekeles; }
    public void setAtlagErtekeles(float atlagErtekeles) { this.atlagErtekeles = atlagErtekeles; }

    public int getErtekelesekSzama() { return ertekelesekSzama; }
    public void setErtekelesekSzama(int ertekelesekSzama) { this.ertekelesekSzama = ertekelesekSzama; }
}