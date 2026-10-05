package pe.lurin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;

/**
 * Consulta la fuente de la ONPE, agrega una lectura a docs/datos.json y termina.
 * Variables de entorno:
 *   ONPE_URL  -> endpoint con los resultados de Lurín (ver README)
 *   DEMO=true -> genera datos FALSOS de prueba (no usar el domingo)
 */
public class Recolector {

    static final ObjectMapper JSON = new ObjectMapper();
    static final Path ARCHIVO = Path.of("docs", "datos.json");

    public static void main(String[] args) throws Exception {
        boolean demo = "true".equalsIgnoreCase(System.getenv("DEMO"));
        ObjectNode raiz = Files.exists(ARCHIVO)
                ? (ObjectNode) JSON.readTree(ARCHIVO.toFile())
                : JSON.createObjectNode();
        ArrayNode lecturas = raiz.has("lecturas") ? (ArrayNode) raiz.get("lecturas") : raiz.putArray("lecturas");

        ObjectNode lectura = demo ? lecturaDemo(lecturas.size()) : lecturaOnpe();
        lectura.put("hora", ZonedDateTime.now(ZoneId.of("America/Lima")).toString());
        lecturas.add(lectura);

        raiz.put("distrito", "Lurín");
        raiz.put("demo", demo);
        raiz.put("actualizado", lectura.get("hora").asText());
        Files.createDirectories(ARCHIVO.getParent());
        JSON.writerWithDefaultPrettyPrinter().writeValue(ARCHIVO.toFile(), raiz);
        System.out.println("Lectura guardada. Total: " + lecturas.size());
    }

    /** Descarga la respuesta de la ONPE y la convierte al formato de la página. */
    static ObjectNode lecturaOnpe() throws Exception {
        String url = System.getenv("ONPE_URL");
        if (url == null || url.isBlank())
            throw new IllegalStateException("Falta la variable ONPE_URL (ver README).");

        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "Mozilla/5.0 (seguimiento-ciudadano-lurin)")
                .header("Accept", "application/json")
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new IllegalStateException("ONPE respondió HTTP " + resp.statusCode());

        return parsear(JSON.readTree(resp.body()));
    }

    /**
     * Formato real de la ONPE:
     * {"data":[{"codigoAgrupacionPolitica":"137","nombreAgrupacionPolitica":"RENOVACIÓN POPULAR PERÚ",
     *           "nombreCandidato":"","porcentajeVotosValidos":28.378,"totalVotosValidos":565}, ...],"success":true}
     * Los códigos 80 (votos en blanco) y 81 (votos nulos) vienen en la misma lista.
     */
    static ObjectNode parsear(JsonNode origen) {
        if (!origen.path("success").asBoolean(false) || !origen.path("data").isArray())
            throw new IllegalStateException("Respuesta inesperada de la ONPE: " + origen.toString().substring(0, Math.min(200, origen.toString().length())));
        ObjectNode out = JSON.createObjectNode();
        ArrayNode cands = out.putArray("candidatos");
        for (JsonNode c : origen.path("data")) {
            String cod = c.path("codigoAgrupacionPolitica").asText();
            if (cod.equals("80")) { out.put("blancos", c.path("totalVotosValidos").asLong()); continue; }
            if (cod.equals("81")) { out.put("nulos", c.path("totalVotosValidos").asLong()); continue; }
            ObjectNode n = cands.addObject();
            n.put("nombre", c.path("nombreAgrupacionPolitica").asText());
            n.put("partido", c.path("nombreCandidato").asText(""));
            n.put("votos", c.path("totalVotosValidos").asLong());
        }
        if (cands.isEmpty()) throw new IllegalStateException("Sin partidos en la respuesta.");
        return out; // actas: pendiente (requiere la URL "totales")
    }

    /** Datos inventados solo para probar la página. */
    static ObjectNode lecturaDemo(int paso) {
        Random r = new Random(7);
        String[][] cs = {{"Candidato A (demo)", "Partido 1"}, {"Candidato B (demo)", "Partido 2"},
                         {"Candidato C (demo)", "Partido 3"}, {"Candidato D (demo)", "Partido 4"}};
        double avance = Math.min(100, (paso + 1) * 6.5);
        ObjectNode out = JSON.createObjectNode();
        out.put("actasProcesadas", Math.round(avance * 10) / 10.0);
        out.put("actasContabilizadas", Math.round(avance * 0.9 * 10) / 10.0);
        ArrayNode cands = out.putArray("candidatos");
        for (int i = 0; i < cs.length; i++) {
            ObjectNode n = cands.addObject();
            n.put("nombre", cs[i][0]);
            n.put("partido", cs[i][1]);
            n.put("votos", Math.round((4000 - i * 700 + r.nextInt(300)) * avance / 100));
        }
        return out;
    }
}
