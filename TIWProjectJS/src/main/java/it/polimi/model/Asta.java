package it.polimi.model;

import java.time.LocalDateTime;
import java.util.List;

public class Asta {
    private int id;
    private double prezzoIniziale;      // primitivo double
    private int rialzoMinimo;
    private LocalDateTime scadenza;
    private boolean chiusa;
    private int venditoreId;
    private Integer vincitoreId;        // wrapper Integer (può essere null)
    private Double prezzoFinale;        // wrapper Double (può essere null)
    private List<Articolo> articoli;
    private Double offertaMassima;      // wrapper Double (può essere null)

    // Costruttori
    public Asta() {}

    public Asta(double prezzoIniziale, int rialzoMinimo, LocalDateTime scadenza, int venditoreId) {
        this.prezzoIniziale = prezzoIniziale;
        this.rialzoMinimo = rialzoMinimo;
        this.scadenza = scadenza;
        this.venditoreId = venditoreId;
        this.chiusa = false;
        this.offertaMassima = null;  // Inizialmente null
    }

    // Getter e Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getPrezzoIniziale() { return prezzoIniziale; }
    public void setPrezzoIniziale(double prezzoIniziale) { this.prezzoIniziale = prezzoIniziale; }

    public int getRialzoMinimo() { return rialzoMinimo; }
    public void setRialzoMinimo(int rialzoMinimo) { this.rialzoMinimo = rialzoMinimo; }

    public LocalDateTime getScadenza() { return scadenza; }
    public void setScadenza(LocalDateTime scadenza) { this.scadenza = scadenza; }

    public boolean isChiusa() { return chiusa; }
    public void setChiusa(boolean chiusa) { this.chiusa = chiusa; }

    public int getVenditoreId() { return venditoreId; }
    public void setVenditoreId(int venditoreId) { this.venditoreId = venditoreId; }

    public Integer getVincitoreId() { return vincitoreId; }
    public void setVincitoreId(Integer vincitoreId) { this.vincitoreId = vincitoreId; }

    public Double getPrezzoFinale() { return prezzoFinale; }
    public void setPrezzoFinale(Double prezzoFinale) { this.prezzoFinale = prezzoFinale; }

    public List<Articolo> getArticoli() { return articoli; }
    public void setArticoli(List<Articolo> articoli) { this.articoli = articoli; }

    // METODO CORRETTO per getOffertaMassima
    public double getOffertaMassima() {
        // Se offertaMassima è null, ritorna il prezzo iniziale
        return offertaMassima != null ? offertaMassima : prezzoIniziale;
    }

    public void setOffertaMassima(Double offertaMassima) {
        this.offertaMassima = offertaMassima;
    }

    // Metodo helper per sapere se ci sono offerte
    public boolean hasOfferte() {
        return offertaMassima != null;
    }

    // Metodo per ottenere il prezzo attuale (massimo tra offerte e prezzo iniziale)
    public double getPrezzoAttuale() {
        return getOffertaMassima();  // Usa il metodo già corretto
    }

    public boolean isScaduta() {
        return LocalDateTime.now().isAfter(scadenza);
    }

    @Override
    public String toString() {
        return "Asta{" +
                "id=" + id +
                ", prezzoIniziale=" + prezzoIniziale +
                ", offertaMassima=" + offertaMassima +
                ", scadenza=" + scadenza +
                ", chiusa=" + chiusa +
                '}';
    }
}