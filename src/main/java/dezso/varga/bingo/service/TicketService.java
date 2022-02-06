package dezso.varga.bingo.service;

import dezso.varga.bingo.domain.Ticket;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TicketService {

    private static final int ROW = 3;
    private static final int COLUMN = 9;
    Map<Integer, List<Integer>> ranges = new HashMap<>();

    public void initRanges() {
        List<Integer> firstRange = IntStream.range(1, 10).boxed().collect(Collectors.toList());
        Collections.shuffle(firstRange);
        ranges.put(1, firstRange);
        IntStream.range(2,9).forEach(rangeNumber -> {
                    List<Integer> range = IntStream.range((rangeNumber-1)*10, (rangeNumber-1)*10 + 10).boxed().collect(Collectors.toList());
                    Collections.shuffle(range);

                    ranges.put(rangeNumber, range);
                });
        List<Integer> lastRange = IntStream.range(80, 91).boxed().collect(Collectors.toList());
        Collections.shuffle(lastRange);
        ranges.put(9, lastRange);

    }

    public int getRandomNumberByRange(int rangeNumber) {
        return ranges.get(rangeNumber).remove(0);
    }

    private void putToRandomColumnsOnRow(int row, List<Ticket> strip) {
        for (Ticket ticket: strip) {
            List<Integer> columns = this.getRandomRangeNumbers(5, new ArrayList<>());
            columns.forEach(column -> ticket.getNumbers()[row][column-1] = this.getRandomNumberByRange(column));
        }
    }

    private void putToFreeColumnsThenRandomOnRow(int row, List<Ticket> strip) {
        for (Ticket ticket: strip) {
            List<Integer> freeColumns = this.getFreeColumnsUntilRow(ticket.getNumbers(), 1);
            List<Integer> columns = this.getRandomRangeNumbers(5 - freeColumns.size(), freeColumns);
            columns.addAll(freeColumns);
            columns.forEach(column -> ticket.getNumbers()[1][column-1] = this.getRandomNumberByRange(column));
        }
    }

    public List<Ticket> generateStrip() {
        this.initRanges();
        List<Ticket> strip =
                Arrays.asList(new Ticket(), new Ticket(), new Ticket(), new Ticket(), new Ticket(), new Ticket());

        this.putToRandomColumnsOnRow(0, strip);

        this.putToFreeColumnsThenRandomOnRow(1, strip);

        this.putToRandomColumnsOnRow(2, strip);

        if (!ranges.isEmpty()) {
            this.placeRemainingNumbers(strip);
        }
        this.repairDueToInvalidColumns(strip);

        this.sortColumns(strip);

        return strip;
    }

    public void repairDueToInvalidColumns(List<Ticket> strip) {

        for (Ticket invalidTicketCandidate:strip) {
            int invalidColumn = this.getInvalidColumn(invalidTicketCandidate);
            if (invalidColumn != -1) {
                for (Ticket ticket: strip) {
                    if (this.switchDueToInvalidColumn(invalidColumn, ticket, invalidTicketCandidate)) {
                        break;
                    }
                }
            }
        }

    }

    public void placeRemainingNumbers(List<Ticket> strip) {
        for (int i=1; i<10; i++) {
            if (!ranges.get(i).isEmpty()) {
                for (Integer value:ranges.get(i)) {
                    this.placeRemainingNumber(value, strip);
                }
            }
        }
    }

    public boolean placeRemainingNumber(int value, List<Ticket> strip) {
        int column = value/10 != 9 ? value/10 : 8;
        Ticket incompleteTicket = this.getFirstIncompleteTicket(strip);
        int incompleteRow = this.getIncompleteRow(incompleteTicket);

        for (Ticket ticket:strip) {
            for (int i=0; i<ROW; i++) {
                int switchableColumn = this.getSwitchableColumn(i, ticket, incompleteTicket);
                if (ticket.getNumbers()[i][column] == 0 && switchableColumn > -1) {
                    ticket.getNumbers()[i][column] = value;
                    incompleteTicket.getNumbers()[incompleteRow][switchableColumn] = ticket.getNumbers()[i][switchableColumn];
                    ticket.getNumbers()[i][switchableColumn] = 0;
                    return true;
                }
            }
        }
        return false;
    }

    public void sortColumns(List<Ticket> strip) {
        List<Integer> column;
        for (Ticket ticket:strip) {
            for (int i=0; i<COLUMN; i++) {
                column = this.getColumnList(i, ticket);
                if (Collections.frequency(column,0) < 2) {
                    int indexOfZero = column.indexOf(0);
                    if (indexOfZero!= -1) {
                        column.remove(indexOfZero);
                    }
                    Collections.sort(column);
                    if (indexOfZero!= -1) {
                        column.add(indexOfZero, Integer.valueOf(0));
                    }
                    for (int j=0; j<ROW; j++) {
                        ticket.getNumbers()[j][i] = column.get(j);
                    }
                }

            }
        }
    }

    public int getInvalidColumn(Ticket ticket) {
        List<Integer> column;
        for (int i=0; i<COLUMN; i++) {
            column = this.getColumnList(i, ticket);

            if ( Collections.frequency(column, 0) == ROW) {
                return i;
            }
        }
        return -1;
    }

    public List<Integer> getColumnList(int column, Ticket ticket) {
        List<Integer> columnList = new ArrayList<>();
        for (int i=0; i<ROW; i++) {
            columnList.add(ticket.getNumbers()[i][column]);
        }
        return columnList;
    }

    public boolean switchDueToInvalidColumn(int invalidColumn, Ticket ticket, Ticket invalidTicket) {
        List<Integer> columnList = this.getColumnList(invalidColumn, ticket);

        for (int i=0; i<ROW; i++) {
            if (ticket.getNumbers()[i][invalidColumn] != 0 && Collections.frequency(columnList, 0) <2) {
                for (int j=0; j<COLUMN; j++) {
                    if (ticket.getNumbers()[i][j] == 0 && invalidTicket.getNumbers()[i][j] != 0
                            && Collections.frequency(this.getColumnList(j, invalidTicket), 0) < 2 ){

                            invalidTicket.getNumbers()[i][invalidColumn] = ticket.getNumbers()[i][invalidColumn];
                            ticket.getNumbers()[i][invalidColumn] = 0;

                            ticket.getNumbers()[i][j] = invalidTicket.getNumbers()[i][j];
                            invalidTicket.getNumbers()[i][j] = 0;
                            return true;
                    }
                }
            }
        }
        return false;
    }

    public int getSwitchableColumn(int row, Ticket ticket, Ticket incompleteTicket) {
        int incompleteRow = this.getIncompleteRow(incompleteTicket);
        List<Integer> column;
        for (int i=0; i<COLUMN; i++) {
            column = new ArrayList<>();
            for (int j=0; j<ROW; j++) {
                column.add(ticket.getNumbers()[j][i]);
            }
            if (ticket.getNumbers()[row][i] != 0 && Collections.frequency(column, 0) <2 &&
                    incompleteTicket.getNumbers()[incompleteRow][i] == 0) {
                return i;
            }
        }
        return -1;
    }

    public Ticket getFirstIncompleteTicket(List<Ticket> strip) {
        for (Ticket ticket:strip) {
            if (this.getIncompleteRow(ticket) > -1) {
                return ticket;
            }
        }
        return null;
    }

    public int getIncompleteRow(Ticket ticket) {
        int rowNumbers;
        for (int i=0; i<ROW; i++) {
            rowNumbers = 0;
            for (int j=0; j<COLUMN; j++) {
                if (ticket.getNumbers()[i][j] != 0) {
                    rowNumbers++;
                }
            }
            if (rowNumbers < 5) {
                return i;
            }
        }
        return -1;
    }

    public List<Integer> getFreeColumnsUntilRow(int[][] ticket, int row) {
        List<Integer> freeColumns = new ArrayList<>();
        boolean isFree;
        for (int i=0; i<COLUMN; i++) {
            isFree = true;
            for (int j=0; j<row; j++) {
                if (ticket[j][i] != 0) {
                    isFree = false;
                }
            }
            if (isFree && this.ranges.get(i+1).size() >0) {
                freeColumns.add(i+1);
            }
        }
        return freeColumns;
    }

    public List<Integer> getRandomRangeNumbers(int count, List<Integer> exclude) {
        List<Integer> range = IntStream.range(1, 10)
                .filter(rangeNumber -> !exclude.contains(rangeNumber) && this.ranges.get(rangeNumber).size() > 0).boxed().collect(Collectors.toList());
        Collections.shuffle(range);
        return range.stream().limit(count).collect(Collectors.toList());
    }

    public void print(List<Ticket> strip) {
        for (Ticket ticket: strip) {
            System.out.println("------------------------------------");
            for (int i=0; i<ROW; i++) {
                for (int j=0; j<COLUMN; j++) {
                    System.out.printf("%-3s ", ticket.getNumbers()[i][j]);
                }
                System.out.println();
            }
        }
        System.out.println("------------------------------------");
    }

}
