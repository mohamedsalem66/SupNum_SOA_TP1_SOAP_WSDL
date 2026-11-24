package mr.supnum.bib.impl;

import com.sun.xml.ws.developer.SchemaValidation;
import jakarta.jws.WebService;
import mr.supnum.bib.contrat.EmpruntPortType;
import mr.supnum.bib.contrat.EmpruntType;
import mr.supnum.bib.contrat.LivreIndisponible;
import mr.supnum.bib.contrat.LivreIndisponibleFault;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

@SchemaValidation
@WebService(serviceName = "EmpruntService", portName = "EmpruntPort",
        targetNamespace = "http://supnum.mr/bib",
        endpointInterface = "mr.supnum.bib.contrat.EmpruntPortType",
        wsdlLocation = "wsdl/EmpruntService.wsdl")
public class EmpruntServiceImpl implements EmpruntPortType {
    @Override
    public EmpruntType emprunterLivre(String idAdherent, String isbn)
            throws LivreIndisponibleFault {
        if (isbn.equals("978-2-10-074444-4")) {
            LivreIndisponible info = new LivreIndisponible();
            info.setIsbn(isbn);
            info.setRetourPrevu(date(LocalDate.of(2026, 12, 2)));
            throw new LivreIndisponibleFault("Livre indisponible", info);
        }
        EmpruntType emprunt = new EmpruntType();
        emprunt.setNumero("E-2026-0812");
        emprunt.setDateRetour(date(LocalDate.now().plusDays(14)));
        return emprunt;
    }

    private static XMLGregorianCalendar date(LocalDate date) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(date.toString());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
