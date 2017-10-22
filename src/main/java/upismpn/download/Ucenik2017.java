package upismpn.download;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static upismpn.UpisMpn.DEBUG;

/**
 * Created by luka on 3.7.17..
 */
public class Ucenik2017 extends Ucenik {

    private static final String UCENICI_URL = "http://upis.mpn.gov.rs/Lat/Ucenici/";

    public Ucenik2017(String id) {
        super(id);
        exists = exists && new File(DownloadController.DATA_FOLDER, id + ".json").exists();
    }

    protected String osId;
    protected String upisana;
    protected String jsonData;
    protected String bodovaAM;
    protected String blizanac, najboljiBlizanacBodovi;
    protected String maternji, prviStrani, drugiStrani;
    protected String origUkupnoBodova, origKrug;
    protected boolean prioritet;

    //naziv testa -> broj bodova
    protected Map<String, String> prijemni = new HashMap<>();

    protected List<Profil> profili = new ArrayList<>();
    protected List<Zelja> listaZelja1 = new ArrayList<>();
    protected List<Zelja> listaZelja2 = new ArrayList<>();

    public String getOsId() {
        return osId;
    }

    public String getJsonData() {
        return jsonData;
    }

    public String getBodovaAM() {
        return bodovaAM;
    }

    public String getBlizanac() {
        return blizanac;
    }

    public String getNajboljiBlizanacBodovi() {
        return najboljiBlizanacBodovi;
    }

    public String getMaternji() {
        return maternji;
    }

    public String getPrviStrani() {
        return prviStrani;
    }

    public String getDrugiStrani() {
        return drugiStrani;
    }

    public Map<String, String> getPrijemni() {
        return prijemni;
    }

    public List<Zelja> getListaZelja1() {
        return listaZelja1;
    }

    public List<Zelja> getListaZelja2() {
        return listaZelja2;
    }

    public String getUpisana() {
        return upisana;
    }

    public boolean isPrioritet() {
        return prioritet;
    }


    public Ucenik setDetails(String ukBodova, String krug) {
        origUkupnoBodova = ukBodova;
        prioritet = ukBodova.startsWith("+");
        this.origKrug = krug;
        return this;
    }

    public static class Zelja {
        private String sifraSmera, uslov, bodovaZaUpis;

        public Zelja(String sifraSmera, String uslov, String bodovaZaUpis) {
            this.sifraSmera = sifraSmera;
            this.uslov = uslov;
            this.bodovaZaUpis = bodovaZaUpis;
        }
        public Zelja(String compactString) {
            String[] tokens = compactString.split(",", -1);
            sifraSmera = tokens[0];
            uslov = tokens[1];
            bodovaZaUpis = tokens[2];
        }
        public String getSifraSmera() {
            return sifraSmera;
        }
        public String getUslov() {
            return uslov;
        }
        public String getBodovaZaUpis() {
            return bodovaZaUpis;
        }

        @Override
        public String toString() {
            return sifraSmera + "," + uslov + "," + bodovaZaUpis;
        }
    }

    public static class Profil {
        private String naziv, prijemni, takmicenje, ukupno;

        public Profil(String naziv, String prijemni, String takmicenje, String ukupno) {
            this.naziv = naziv;
            this.takmicenje = takmicenje;
            this.prijemni = prijemni;
            this.ukupno = ukupno;
        }
        public Profil(String compactString) {
            String[] tokens = compactString.split(",", -1);
            naziv = tokens[0];
            prijemni = tokens[1];
            takmicenje = tokens[2];
            ukupno = tokens[3];
        }

        @Override
        public String toString() {
            return naziv + "," + prijemni + "," + takmicenje + "," + ukupno;
        }
    }

    @Override
    public Ucenik loadFromNet() throws IOException {
        if (DEBUG) {
            System.out.println("loading ucenik: " + id);
        }
        if(exists && !OVERWRITE_OLD) return this;

        Document doc = Jsoup.connect(UCENICI_URL + id).get();
        Elements scripts = doc.getElementsByTag("script");
        String script = scripts.get(scripts.size()-3).data();
        parseJson(script);

        return this;
    }

    public Ucenik2017 loadFromJson() throws IOException {
        if(!OVERWRITE_OLD) System.err.println("warning: OVERWRITE_OLD is set to false; changes won't persist");
        File f = new File(DownloadController.DATA_FOLDER, id + ".json");
        parseJson("\r\n" + new String(Files.readAllBytes(f.toPath())).replace('\r', '\n'));
        return this;
    }

    private void parseJson(String script) throws IOException {
        String[] data = script.split("\\n", 12);
        //data[0] is only \r
        String basic = data[1].split(" = ")[1].trim().replace("];", "]");
        String sesti = data[2].split(" = ")[1].trim().replace("];", "]");
        String sedmi = data[3].split(" = ")[1].trim().replace("];", "]");
        String osmi  = data[4].split(" = ")[1].trim().replace("];", "]");
        String nagrade = data[5].split(" = ")[1].trim().replace("];", "]");
        String prijemni = data[6].split(" = ")[1].trim().replace("];", "]");
        String profili = data[7].split(" = ")[1].trim().replace("];", "]");
        String zelje = data[8].split(" = ")[1].trim().replace("];", "]");
        String zelje2 = data[9].split(" = ")[1].trim().replace("];", "]");
        parseBasic(basic);
        OsnovneDownloader2017.getInstance().addOsnovna(Integer.parseInt(osId));
        sestiRaz = parseOcene(sesti);
        sedmiRaz = parseOcene(sedmi);
        osmiRaz = parseOcene(osmi);
        parseNagrade(nagrade);
        parsePrijemni(prijemni);
        parseProfili(profili);
        parseZelje(zelje, listaZelja1);
        parseZelje(zelje2, listaZelja2);

        StringBuilder sb = new StringBuilder();
        for(int i=1; i<10; i++) sb.append(data[i]).append("\n");
        jsonData = sb.toString();
    }

    private void parseBasic(String json) {
        JsonObject data = new JsonParser().parse(json).getAsJsonArray().get(0).getAsJsonObject();
        osId = data.get("IDSKola").getAsString();
        upisana = data.get("UpisanNa").getAsString();
        srpski = data.get("BodovaSrp").getAsString();
        matematika = data.get("BodovaMat").getAsString();
        kombinovani = data.get("BodovaKom").getAsString();
        bodovaAM = data.get("BodovaAM").getAsString();
        ukupnoBodova = data.get("BodovaUkupno").getAsString();
        maternji = data.get("Maternji").getAsString();
        prviStrani = data.get("PrviStraniJezik").getAsString();
        drugiStrani = data.get("DrugiStraniJezik").getAsString();
        blizanac = data.get("blizanac").getAsString();
        najboljiBlizanacBodovi = data.get("NajboljiBlizanacBodovi").getAsString(); //I have no idea what this is

        String krugText = data.get("UpisanNaOpis").getAsString();
        if(krugText.startsWith("Raspoređen u prvom") ||
                krugText.startsWith("Распоређен у првом")) krug = "1";
        else if(krugText.startsWith("Raspoređen u drugom") ||
                krugText.startsWith("Распоређен у другом")) krug = "2";
        else if(krugText.startsWith("Upisan po odluci OUK") ||
                krugText.startsWith("Уписан по одлуци ОУК")) krug = "*";
        else throw new IllegalArgumentException("Invalid krug text: " + krugText + " @ " + id);
    }

    private Map<String, String> parseOcene(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> map = gson.fromJson(json.substring(1, json.length()-1), type);
        map.remove("IDUcenik");
        return map;
    }

    private void parseNagrade(String json) {
        JsonArray data = new JsonParser().parse(json).getAsJsonArray();
        for(JsonElement el : data) {
            JsonObject obj = el.getAsJsonObject();
            takmicenja.put(obj.get("nagradaPredmet").getAsString() + "~" + obj.get("nagradaNivo").getAsString()
                    + "~" + obj.get("nagradaMesto").getAsString(),
                    obj.get("nagradaBodova").getAsString());
        }
    }

    private void parsePrijemni(String json) {
        JsonArray data = new JsonParser().parse(json).getAsJsonArray();
        for(JsonElement el : data) {
            JsonObject obj = el.getAsJsonObject();
            prijemni.put(obj.get("Prijemni").getAsString().trim(), obj.get("Bodova").getAsString());
        }
    }

    private void parseProfili(String json) {
        JsonArray data = new JsonParser().parse(json).getAsJsonArray();
        for(JsonElement el : data) {
            JsonObject obj = el.getAsJsonObject();
            profili.add(new Profil(obj.get("Profil").getAsString(), obj.get("Bodova").getAsString(),
                    obj.get("BodovaNagrade").getAsString(), obj.get("Ukupno").getAsString()));
        }
    }

    private void parseZelje(String json, List<Zelja> zelje) {
        JsonArray data = new JsonParser().parse(json).getAsJsonArray();
        for(JsonElement el : data) {
            JsonObject obj = el.getAsJsonObject();
            zelje.add(new Zelja(obj.get("sifra").getAsString(), obj.get("IspunioUslov").getAsString(),
                    obj.get("sBodova").getAsString()));
        }
    }

    @Override
    public String toCompactString() {
        StringBuilder sb = new StringBuilder();
        sb.append(osId).append("\\").append(upisana).append("\\").append(origKrug).append("\\").append(krug).append("\\").append(blizanac).append("\\").append(najboljiBlizanacBodovi).append("\\").append(prioritet).append("\n");
        sb.append(srpski).append("\\").append(matematika).append("\\").append(kombinovani).append("\\").append(bodovaAM).append("\\").append(origUkupnoBodova).append("\\").append(ukupnoBodova).append("\n");
        sb.append(maternji).append("\\").append(prviStrani).append("\\").append(drugiStrani).append("\n");
        sb.append(UcenikUtils.mapToStringBuilder(UcenikUtils.PredmetiDefault.compress(sestiRaz)));
        sb.append(UcenikUtils.mapToStringBuilder(UcenikUtils.PredmetiDefault.compress(sedmiRaz)));
        sb.append(UcenikUtils.mapToStringBuilder(UcenikUtils.PredmetiDefault.compress(osmiRaz)));
        sb.append(UcenikUtils.mapToStringBuilder(takmicenja));
        sb.append(UcenikUtils.mapToStringBuilder(prijemni));
        sb.append(UcenikUtils.listToStringBuilder(profili));
        sb.append(UcenikUtils.listToStringBuilder(listaZelja1));
        sb.append(UcenikUtils.listToStringBuilder(listaZelja2));
        return sb.toString();
    }

    @Override
    public void loadFromString(String compactString) {
        String[] chunks = compactString.split("\n", -1);

        String[] basics = chunks[0].split("\\\\", -1);
        String[] bodovi = chunks[1].split("\\\\");
        String[] jezici = chunks[2].split("\\\\", -1);
        String[] sesti = chunks[3].split("\\\\", 0);
        String[] sedmi = chunks[4].split("\\\\", 0);
        String[] osmi = chunks[5].split("\\\\", 0);
        String[] takmicenja = chunks[6].split("\\\\", 0);
        String[] prijemni = chunks[7].split("\\\\", 0);
        String[] profili = chunks[8].split("\\\\", 0);
        String[] zelje1 = chunks[9].split("\\\\", 0);
        String[] zelje2 = chunks[10].split("\\\\", 0);

        osId = basics[0];
        upisana = basics[1];
        origKrug = basics[2];
        krug = basics[3];
        blizanac = basics[4];
        najboljiBlizanacBodovi = basics[5];
        prioritet = Boolean.parseBoolean(basics[6]);
        srpski = bodovi[0];
        matematika = bodovi[1];
        kombinovani = bodovi[2];
        bodovaAM = bodovi[3];
        origUkupnoBodova = bodovi[4];
        ukupnoBodova = bodovi[5];
        maternji = jezici[0];
        prviStrani = jezici[1];
        drugiStrani = jezici[2];

        sestiRaz = UcenikUtils.PredmetiDefault.decompress(UcenikUtils.stringArrayToMap(sesti));
        sedmiRaz = UcenikUtils.PredmetiDefault.decompress(UcenikUtils.stringArrayToMap(sedmi));
        osmiRaz = UcenikUtils.PredmetiDefault.decompress(UcenikUtils.stringArrayToMap(osmi));
        this.takmicenja = UcenikUtils.stringArrayToMap(takmicenja);
        this.prijemni = UcenikUtils.stringArrayToMap(prijemni);
        this.profili = UcenikUtils.stringToListProfil(profili);
        listaZelja1 = UcenikUtils.stringToListZelja(zelje1);
        listaZelja2 = UcenikUtils.stringToListZelja(zelje2);
    }

    @Override
    public void saveToFile(File folder) {
        super.saveToFile(folder);
        if(exists && !OVERWRITE_OLD) return;
        File f = new File(folder, id + ".json");
        try (Writer fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"))) {
            fw.write(jsonData);
        } catch (IOException ex) {
            Logger.getLogger(Ucenik.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void saveToFile(File folder, boolean skipJson) {
        if(skipJson) super.saveToFile(folder);
        else this.saveToFile(folder);
    }
}