package it.polimi.model;


public class Articolo {
    private int id;
    private String codice;
    private String nome;
    private String descrizione;
    private String immagine;
    private double prezzo;
    private int proprietarioId;
    private boolean venduto;

    // Costruttori
    public Articolo() {}

    public Articolo(String codice, String nome, String descrizione, String immagine,
                    double prezzo, int proprietarioId) {
        this.codice = codice;
        this.nome = nome;
        this.descrizione = descrizione;
        this.immagine = immagine;
        this.prezzo = prezzo;
        this.proprietarioId = proprietarioId;
        this.venduto = false;
    }

    // Getter e Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodice() { return codice; }
    public void setCodice(String codice) { this.codice = codice; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public String getImmagine() { return immagine; }
    public void setImmagine(String immagine) { this.immagine = immagine; }

    public double getPrezzo() { return prezzo; }
    public void setPrezzo(double prezzo) { this.prezzo = prezzo; }

    public int getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(int proprietarioId) { this.proprietarioId = proprietarioId; }

    public boolean isVenduto() { return venduto; }
    public void setVenduto(boolean venduto) { this.venduto = venduto; }
}