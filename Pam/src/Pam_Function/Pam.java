package Pam_Function;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Pam {

    // Dati di connessione al database
    private static String dbUrl = "jdbc:mariadb://localhost:3306/JACMARE";
    private static String user = "root";
    private static String pwd = "ciao";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Chiedi il codice anagrafico all'utente
        System.out.print("Inserisci il codice anagrafico: ");	//
        String codiceAnagrafico = scanner.nextLine();

        // Dati iniziali per il primo prestito
        float Capitale_iniziale = 101000;
        int DurataMesi = 6;
        float TassoAnnuale = 24;

        // Data di esecuzione (usiamo la data corrente)
        LocalDate dataEsecuzione = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Generazione dei 10 prestiti con incremento di capitale e decremento del tasso
        for (int i = 0; i < 10; i++) {
            // Calcolo del tasso mensile
            float TassoMensile = (TassoAnnuale - (i * 2)) / 12 / 100;

            // Codice prestito unico per il cliente
            String codicePrestito = codiceAnagrafico + "_PAM_" + (i + 1);

            // Salva i dati di default nella tabella jac_ctr_base
            salvaDatiBase(
                codiceAnagrafico,
                dataEsecuzione.format(dateFormatter),
                Capitale_iniziale + i * 100,
                codicePrestito,
                DurataMesi + i * 6,
                TassoAnnuale - (i * 2)
            );

            // Visualizza i dettagli del prestito corrente
            System.out.println("\nPiano di Ammortamento - Prestito " + (i + 1));
            System.out.println("Codice Anagrafico: " + codiceAnagrafico);
            System.out.println("Codice Prestito: " + codicePrestito);
            System.out.println("Data di Esecuzione: " + dataEsecuzione.format(dateFormatter));
            System.out.println("Capitale: " + (Capitale_iniziale + i * 100) + " Euro");
            System.out.println("Durata: " + (DurataMesi + i * 6) + " mesi");
            System.out.println("Tasso: " + (TassoAnnuale - (i * 2)) + "% Annui");
            System.out.println("-------------------------------------------------------------------------------");

            // Calcola e mostra il piano di ammortamento per ogni prestito
            generaPianoAmmortamento(
                Capitale_iniziale + i * 100,
                DurataMesi + i * 6,
                TassoMensile,
                codicePrestito,
                dataEsecuzione
            );
        }

        scanner.close();
    }

    public static void generaPianoAmmortamento(float capitale, int durataMesi, float tassoMensile, String codicePrestito, LocalDate dataEsecuzione) {
        // Calcolo della rata mensile (formula metodo francese)
        float rata = (float) (capitale * tassoMensile / (1 - Math.pow(1 + tassoMensile, -durataMesi)));

        // Stampa l'intestazione del piano di ammortamento
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("%-10s %-15s %-15s %-15s %-15s %-15s%n", "Mese", "Data", "Rata", "Quota Capitale", "Quota Interesse", "Capitale Residuo");

        float capitaleResiduo = capitale;

        // Loop per generare il piano di ammortamento per ogni mese
        for (int mese = 1; mese <= durataMesi; mese++) {
            float interesse = capitaleResiduo * tassoMensile;
            float quotaCapitale = rata - interesse;
            capitaleResiduo -= quotaCapitale;

            // Calcolo della data della rata
            LocalDate dataRata = dataEsecuzione.plusMonths(mese);

            // Stampa i dettagli per il mese corrente
            System.out.printf("%-10d %-15s %-15.2f %-15.2f %-15.2f %-15.2f%n",
                mese,
                dataRata.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                rata,
                quotaCapitale,
                interesse,
                Math.max(capitaleResiduo, 0)
            );

            // Salvataggio dei dati nel database
            salvaNelDatabase(codicePrestito, mese, rata, quotaCapitale, interesse, Math.max(capitaleResiduo, 0), dataRata.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }

        // Stampa la fine del piano
        System.out.println("-------------------------------------------------------------------------------");
    }

    public static void salvaDatiBase(String codiceAnagrafico, String dataEsecuzione, float capitaleIniziale, String codicePrestito, int durata, float tasso) {
        // Connessione al database e query per inserire i dati di default
        try (Connection conn = DriverManager.getConnection(dbUrl, user, pwd)) {
            String sql = "INSERT INTO jac_ctr_base (CTR_COD_ANAGRAFICO, CTR_DATA, CTR_IMPORTO, CTR_COD_PRESTITO, CTR_DURATA, CTR_TASSO) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, codiceAnagrafico);
                stmt.setString(2, dataEsecuzione);
                stmt.setFloat(3, capitaleIniziale);
                stmt.setString(4, codicePrestito);
                stmt.setInt(5, durata);
                stmt.setFloat(6, tasso);

                stmt.executeUpdate(); // Esegui l'inserimento
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void salvaNelDatabase(String codicePrestito, int numeroRata, float rata, float quotaCapitale, float quotaInteresse, float capitaleResiduo, String dataRata) {
        // Connessione al database e query per inserire i dati delle rate
        try (Connection conn = DriverManager.getConnection(dbUrl, user, pwd)) {
            String sql = "INSERT INTO jac_ctr_pam (CTRP_COD_PRESTITO, CTRP_NUMERO_RATA, CTRP_IMPORTO_RATA, CTRP_QUOTA_CAPITALE, CTRP_QUOTA_INTERESSI, CTRP_CAPITALE_RESIDUO) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, codicePrestito);
                stmt.setInt(2, numeroRata);
                stmt.setFloat(3, rata);
                stmt.setFloat(4, quotaCapitale);
                stmt.setFloat(5, quotaInteresse);
                stmt.setFloat(6, capitaleResiduo);


                stmt.executeUpdate(); // Esegui l'inserimento
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}