package dezso.varga.bingo;

import dezso.varga.bingo.domain.Ticket;
import dezso.varga.bingo.service.TicketService;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@Profile("!test")
@AllArgsConstructor
public class BingoCLIRunner implements CommandLineRunner {

    private TicketService ticketService;

    @Override
    public void run(String... args) {

        long millisBefore = new Date().getTime();
        for (int i=0;i<10000;i++) {
            List<Ticket> strip = ticketService.generateStrip();
//            ticketService.print(strip);
        }

        long millisAfter = new Date().getTime();

        System.out.println("Duration: " + (millisAfter - millisBefore) + " milliseconds");
    }
}
