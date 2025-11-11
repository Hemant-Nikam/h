// DEVELOPED BY : HEMANT NIKAM
import java.io.*;
import java.util.*;

// ===== SYMBOL TABLE ENTRY =====
class Symbol {
    static int cnt = 1;  // Auto-increment index
    int index;
    int address = 0;
    boolean defined = false;

    Symbol(int address, boolean defined) {
        this.index = cnt;
        this.address = address;
        this.defined = defined;
    }
}

// ===== LITERAL TABLE ENTRY =====
class Literal {
    static int cnt = 1;
    int index;
    int address = 0;

    Literal(int address) {
        this.index = cnt;
        this.address = address;
    }
}

// ===== MAIN CLASS =====
public class Pass1 {
    static int lc = 0; // Location Counter

    // Tables
    static Map<String, Symbol> symtab = new LinkedHashMap<>();
    static Map<String, Literal> littab = new LinkedHashMap<>();
    static ArrayList<Integer> pooltab = new ArrayList<>();
    static ArrayList<String> fref = new ArrayList<>(); // forward reference list

    // Instruction sets
    static Map<String, String> IS = Map.ofEntries(
        Map.entry("STOP", "(IS,00)"),
        Map.entry("ADD", "(IS,01)"),
        Map.entry("SUB", "(IS,02)"),
        Map.entry("MULT", "(IS,03)"),
        Map.entry("MOVER", "(IS,04)"),
        Map.entry("MOVEM", "(IS,05)"),
        Map.entry("COMP", "(IS,06)"),
        Map.entry("BC", "(IS,07)"),
        Map.entry("DIV", "(IS,08)"),
        Map.entry("READ", "(IS,09)"),
        Map.entry("PRINT", "(IS,10)")
    );

    static Map<String, String> REG = Map.ofEntries(
        Map.entry("AREG,", "(REG,1)"),
        Map.entry("BREG,", "(REG,2)"),
        Map.entry("CREG,", "(REG,3)"),
        Map.entry("DREG,", "(REG,4)")
    );

    static Map<String, String> DL = Map.ofEntries(
        Map.entry("DC", "(DL,01)"),
        Map.entry("DS", "(DL,02)")
    );

    static Map<String, String> AD = Map.ofEntries(
        Map.entry("START", "(AD,01)"),
        Map.entry("END", "(AD,02)"),
        Map.entry("LTORG", "(AD,03)"),
        Map.entry("ORIGIN", "(AD,04)"),
        Map.entry("EQU", "(AD,05)")
    );

    static Map<String, String> CC = Map.ofEntries(
        Map.entry("LT,", "(CC,01)"),
        Map.entry("GT,", "(CC,02)"),
        Map.entry("EQ,", "(CC,03)"),
        Map.entry("LE,", "(CC,04)"),
        Map.entry("GE,", "(CC,05)"),
        Map.entry("ANY,", "(CC,06)")
    );

    public static void main(String args[]) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader("input.txt"));
        BufferedWriter bw = new BufferedWriter(new FileWriter("ic.txt"));
        BufferedWriter sym = new BufferedWriter(new FileWriter("symtab.txt"));
        BufferedWriter lit = new BufferedWriter(new FileWriter("littab.txt"));
        BufferedWriter pool = new BufferedWriter(new FileWriter("pooltab.txt"));

        String line = null;
        boolean flag = false;
        pooltab.add(0); // start of pool

        // MAIN LOOP
        while (flag || ((line = br.readLine()) != null)) {
            flag = false;
            String parts[] = line.trim().split("\\s+");
            String var = parts[0];

            // ====== START ======
            if (var.equals("START")) {
                lc = Integer.parseInt(parts[1]);
                bw.write(AD.get("START") + "(C," + lc + ")\n");
            }

            // ====== ORIGIN ======
            else if (var.equals("ORIGIN")) {
                if (parts.length == 2) {
                    lc = Integer.parseInt(parts[1]);
                    bw.write(AD.get(var) + "(C," + lc + ")\n");
                } else {
                    if (symtab.containsKey(parts[1])) {
                        // If symbol is already defined
                        if (symtab.get(parts[1]).address > 0) {
                            if (parts[2].equals("-")) {
                                lc = symtab.get(parts[1]).address - Integer.parseInt(parts[3]);
                            } else {
                                lc = symtab.get(parts[1]).address + Integer.parseInt(parts[3]);
                            }
                            bw.write(AD.get(var) + "(S," + symtab.get(parts[1]).index + ")(C," + parts[3] + ")\n");
                        } else {
                            // Forward reference (symbol yet undefined)
                            String expr = parts[1] + " " + parts[2] + " " + parts[3];
                            fref.add(expr);
                            bw.write(AD.get(var) + "(S,)(C," + parts[3] + ")\n");
                        }
                    } else {
                        // Symbol not present at all → create forward ref
                        String expr = parts[1] + " " + parts[2] + " " + parts[3];
                        fref.add(expr);
                        symtab.put(parts[1], new Symbol(0, false));
                        bw.write(AD.get(var) + "(S," + parts[1] + ")(C," + parts[3] + ")\n");
                    }
                }
            }

            // ====== LTORG or END ======
            else if (var.equals("LTORG") || var.equals("END")) {
                bw.write(AD.get(var));
                int idx = 0;

                // Assign address to unassigned literals
                for (String key : littab.keySet()) {
                    if (littab.get(key).address == 0) {
                        littab.get(key).address = lc++;
                        idx++;
                    }
                }

                pooltab.add(pooltab.get(pooltab.size() - 1) + idx);
                bw.write("\n");
            }

            // ====== DC / DS ======
            else if (var.equals("DS") || var.equals("DC")) {
                if (var.equals("DC")) {
                    lc = lc + 1;
                } else {
                    lc = lc + Integer.parseInt(parts[1]);
                }
                bw.write(DL.get(var) + "(C," + parts[1] + ")\n");
            }

            // ====== INSTRUCTION ======
            else if (IS.containsKey(var)) {
                bw.write(IS.get(var));
                if (parts.length > 1) {
                    for (int i = 1; i < parts.length; i++) {
                        if (REG.containsKey(parts[i])) {
                            bw.write(REG.get(parts[i]));
                        } else if (CC.containsKey(parts[i])) {
                            bw.write(CC.get(parts[i]));
                        } else if (parts[i].startsWith("=")) {
                            // LITERAL
                            if (littab.containsKey(parts[i])) {
                                bw.write("(L," + littab.get(parts[i]).index + ")");
                            } else {
                                littab.put(parts[i], new Literal(0));
                                bw.write("(L," + littab.get(parts[i]).index + ")");
                                Literal.cnt++;
                            }
                        } else {
                            // SYMBOL
                            if (symtab.containsKey(parts[i])) {
                                bw.write("(S," + symtab.get(parts[i]).index + ")");
                            } else {
                                symtab.put(parts[i], new Symbol(0, false));
                                bw.write("(S," + symtab.get(parts[i]).index + ")");
                                Symbol.cnt++;
                            }
                        }
                    }
                }
                bw.write("\n");
                lc++;
            }

            // ====== LABEL HANDLING (symbol:) ======
            else {
                var = var.replace(":", ""); // Remove ':'

                if (symtab.containsKey(var)) {
                    bw.write("(S," + symtab.get(var).index + ")");
                    symtab.get(var).address = lc;
                    symtab.get(var).defined = true;
                } else {
                    // New symbol definition
                    symtab.put(var, new Symbol(lc, true));
                    Symbol.cnt++;
                    bw.write("(S," + symtab.get(var).index + ")");
                }

                // ===== Check for forward references =====
                for (int i = 0; i < fref.size(); i++) {
                    String expr[] = fref.get(i).split("\\s+");
                    if (expr[0].equals(var)) {
                        int op1 = symtab.get(expr[0]).address;
                        int op2 = Integer.parseInt(expr[2]);
                        if (expr[1].equals("+")) {
                            lc = op1 + op2;
                        } else {
                            lc = op1 - op2;
                        }
                    }
                }

                // ===== Handle DC / DS after label =====
                if (parts[1].equals("DC")) {
                    bw.write(DL.get(parts[1]));
                    bw.write("(C," + parts[2] + ")\n");
                    lc = lc + 1;
                } else if (parts[1].equals("DS")) {
                    bw.write(DL.get(parts[1]));
                    bw.write("(C," + parts[2] + ")\n");
                    lc = lc + Integer.parseInt(parts[2]);
                } else {
                    line = "";
                    for (int j = 1; j < parts.length; j++) {
                        line = line + parts[j] + " ";
                    }
                    flag = true;
                }
            }
        }

        // ===== WRITE TABLES =====
        br.close();
        bw.close();

        for (Map.Entry<String, Symbol> entry : symtab.entrySet()) {
            sym.write(entry.getValue().index + " " + entry.getKey() + " " + entry.getValue().address + "\n");
        }
        sym.close();

        for (Map.Entry<String, Literal> entry : littab.entrySet()) {
            lit.write(entry.getValue().index + " " + entry.getKey() + " " + entry.getValue().address + "\n");
        }
        lit.close();

        for (int i = 0; i < pooltab.size(); i++) {
            pool.write(i + " " + pooltab.get(i) + "\n");
        }
        pool.close();
    }
}
