package rs.lukaj.upisstats.scraper.download;

import rs.lukaj.upisstats.scraper.obrada2017.LetterUtils;

/**
 * Predstavlja podatke o jednom smeru; sifru, podrucje i kvotu
 * @author Luka
 */
public class Smer {
    private final String sifra;
    private final String podrucje;
    private final String kvota;
    
    public Smer(String sifra, String podrucje, String kvota) {
        this.sifra = sifra.trim();
        this.podrucje = podrucje;
        this.kvota = kvota;
    }
    
    public Smer(String compactString) {
        String[] tokens = compactString.split("\\\\");
        sifra = tokens[0];
        podrucje = LetterUtils.toLatin(tokens[1].trim());
        kvota = tokens[2];
    }
    
    public String getSifra() {return sifra;}
    public String getPodrucje() {return podrucje;}
    public String getKvota() {return kvota;}
    public String toCompactString() {return sifra.trim() + "\\" + podrucje + "\\" + kvota + "\n";}
}