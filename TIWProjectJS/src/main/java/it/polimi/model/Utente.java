package it.polimi.model;

public class Utente {
    private int id;
    private String username;
    private String password;
    private String nome;
    private String cognome;
    private String indirizzo;

    // Costruttori
    public Utente() {}

    public Utente(String username, String password, String nome, String cognome, String indirizzo) {
        this.username = username;
        this.password = password;
        this.nome = nome;
        this.cognome = cognome;
        this.indirizzo = indirizzo;
    }

    // Getter e Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }

    public String getIndirizzo() { return indirizzo; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }

    public String getNomeCompleto() {
        return nome + " " + cognome;
    }
}