package it.polimi.model;

import java.time.LocalDateTime;

public class Offerta {
    private int id;
    private int astaId;
    private int offerenteId;
    private double importo;
    private LocalDateTime dataOfferta;
    private String nomeOfferente;

    // Costruttori
    public Offerta() {}

    public Offerta(int astaId, int offerenteId, double importo) {
        this.astaId = astaId;
        this.offerenteId = offerenteId;
        this.importo = importo;
        this.dataOfferta = LocalDateTime.now();
    }

    // Getter e Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAstaId() { return astaId; }
    public void setAstaId(int astaId) { this.astaId = astaId; }

    public int getOfferenteId() { return offerenteId; }
    public void setOfferenteId(int offerenteId) { this.offerenteId = offerenteId; }

    public double getImporto() { return importo; }
    public void setImporto(double importo) { this.importo = importo; }

    public LocalDateTime getDataOfferta() { return dataOfferta; }
    public void setDataOfferta(LocalDateTime dataOfferta) { this.dataOfferta = dataOfferta; }

    public String getNomeOfferente() { return nomeOfferente; }
    public void setNomeOfferente(String nomeOfferente) { this.nomeOfferente = nomeOfferente; }
}