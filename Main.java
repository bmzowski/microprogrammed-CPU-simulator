import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Created by Brandon Kozlowski
 */
public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        //File IO
        File file = new File("microsim.txt");
        Scanner input = new Scanner(file);
        String value;
        int j = 0;


        //Registers
        int PC = 0, MAR = 0;                    //9-bit registers
        int IR = 0, MDR = 0, ACC = 0, TMP = 0;  //12-bit registers

        boolean sentinel = false;

        //Microprogrammed control
        int CSAR = 0;                       //5 bits
        int CSIR;                       //22 bits
        int cs[][] = new int[32][18];   //Array for control store

        //Memory
        int mem[] = new int[512]; //Contains 12-bit words

        //Decoding table
        int dTable[] = new int[8];

        //Define bits for control store
        int ACC_IN = 0;
        int ACC_OUT = 1;
        int ALU_ADD = 2;
        int ALU_SUB = 3;
        int IR_IN = 4;
        int IR_OUT = 5;
        int MAR_IN = 6;
        int MDR_IN = 7;
        int MDR_OUT = 8;
        int PC_IN = 9;
        int PC_OUT = 10;
        int PC_INCR = 11;
        int READ = 12;
        int TMP_OUT = 13;
        int WRITE = 14;
        int BRTABLE = 15;
        int NEXT = 16;
        int OR_ADDR = 17;

        //implement control store grid
        cs[ 0][ PC_OUT] = 1; cs[ 0][ MAR_IN] = 1; cs[ 0][NEXT] =  2;
        /* special taken branch entry */
        cs[ 1][ IR_OUT] = 1; cs[ 1][  PC_IN] = 1; cs[ 1][NEXT] =  0;
        /* rest of ifetch */
        cs[ 2][PC_INCR] = 1; cs[ 2][   READ] = 1; cs[ 2][NEXT] =  3;
        cs[ 3][MDR_OUT] = 1; cs[ 3][  IR_IN] = 1; cs[ 3][NEXT] =  4;
        cs[ 4][BRTABLE] = 1;                      cs[ 4][NEXT] =  0;
        cs[ 5][ MAR_IN] = 1; cs[ 5][ IR_OUT] = 1; cs[ 5][NEXT] =  6;
        cs[ 6][   READ] = 1;                      cs[ 6][NEXT] =  7;
        cs[ 7][ ACC_IN] = 1; cs[ 7][MDR_OUT] = 1; cs[ 7][NEXT] =  0;
        cs[ 8][ MAR_IN] = 1; cs[ 8][ IR_OUT] = 1; cs[ 8][NEXT] =  9;
        cs[ 9][   READ] = 1;                      cs[ 9][NEXT] = 10;
        cs[10][ACC_OUT] = 1; cs[10][ALU_ADD] = 1; cs[10][NEXT] = 11;
        cs[11][ ACC_IN] = 1; cs[11][TMP_OUT] = 1; cs[11][NEXT] =  0;
        cs[12][ MAR_IN] = 1; cs[12][ IR_OUT] = 1; cs[12][NEXT] = 13;
        cs[13][ MDR_IN] = 1; cs[13][ACC_OUT] = 1; cs[13][NEXT] = 14;
        cs[14][  WRITE] = 1;                      cs[14][NEXT] =  0;
        cs[15][OR_ADDR] = 1;                      cs[15][NEXT] =  0;
        //Extended Instructions
        //Sub
        cs[16][ MAR_IN] = 1; cs[16][ IR_OUT] = 1; cs[16][NEXT] = 17;
        cs[17][   READ] = 1;                      cs[17][NEXT] = 18;
        cs[18][ACC_OUT] = 1; cs[18][ALU_SUB] = 1; cs[18][NEXT] = 19;
        cs[19][ ACC_IN] = 1; cs[19][TMP_OUT] = 1; cs[19][NEXT] =  0;
        //jsub
        cs[20][ MDR_IN] = 1; cs[20][ PC_OUT] = 1; cs[20][NEXT] = 21;
        cs[21][  PC_IN] = 1; cs[21][ IR_OUT] = 1; cs[21][NEXT] = 22;
        cs[22][ PC_OUT] = 1; cs[22][ MAR_IN] = 1; cs[22][NEXT] = 23;
        cs[23][PC_INCR] = 1;                      cs[23][NEXT] = 24;
        cs[24][  WRITE] = 1;                      cs[24][NEXT] =  0;
        //jmpi
        cs[25][ MAR_IN] = 1; cs[25][ IR_OUT] = 1; cs[25][NEXT] = 26;
        cs[26][   READ] = 1;                      cs[26][NEXT] = 27;
        cs[27][MDR_OUT] = 1; cs[27][  PC_IN] = 1; cs[27][NEXT] =  0;

        //Implement decoding table
        dTable[0] = 5;
        dTable[1] = 8;
        dTable[2] = 12;
        dTable[3] = 15;
        dTable[4] = 16;
        dTable[5] = 20;
        dTable[6] = 25;

        //Read Hex values from input file
        while(input.hasNext())
        {
            value = input.next();
            if (value.equals("-1")) {
                break;
            } else {
                mem[j] = Integer.parseInt(value, 16);
                j++;
            }
        }

        //Print the first 20 memory values and formatting
        System.out.print("low memory:");
        for(int i = 0; i < 20; i++)
        {
            System.out.print(String.format("%5x", mem[i]));
            if(i == 9){
                System.out.println("");
                System.out.print(String.format("%11s"," "));
            }
        }
        System.out.println("");
        System.out.println("");

        System.out.println("cycle  PC  IR MAR MDR ACC TMP  CSAR          CSIR            cntl signals");
        System.out.println("     +---+---+---+---+---+---+/----//---------------------//---------------/");

        //Continue until HALT encountered
        int m = 1;
        while(!sentinel)
        {
            //Get the control flags
            int flagSum;
            String flags = "";
            for(int i = 0; i < 16; i++)
            {
                flags = flags + cs[CSAR][i];
            }

            flagSum = Integer.parseInt(flags, 2);

            //Print the registers
            System.out.print(String.format("%4d:%4d%4x%4x%4x%4x%4x%6x%18s|%02x|%x  ",m,PC,IR,MAR,MDR,ACC,TMP,CSAR,flags,cs[CSAR][NEXT],cs[CSAR][OR_ADDR]));

            //Get the next address and set to CSAR
            CSAR = cs[CSAR][NEXT];

            int addr = 511; //9 1-bits
            int opcode = 3584; //3 1-bits followed by 9 zeros

            switch(flagSum)
            {
                case 544: //MAR_in  PC_out;
                    MAR = PC;
                    System.out.println(String.format("%-15s","MAR_in PC_out"));
                    break;
                case 1088: //PCin IR_out;
                    PC = addr & IR;
                    System.out.println(String.format("%-15s","PC_in IR_out"));
                    break;
                case 192: //MDR_OUT PC_in;
                    PC = MDR;
                    System.out.println(String.format("%-15s","PC_in MDR_out"));
                    break;
                case 24: //pc_incr read;
                    MDR = mem[MAR];
                    PC++;
                    System.out.println(String.format("%-15s","PC_incr read"));
                    break;
                case 16: //pc_incr
                    PC++;
                    System.out.println(String.format("%-15s","PC_incr"));
                    break;
                case 2176: //IR_in   MDR_out;
                    IR = MDR;
                    System.out.println(String.format("%-15s","IR_in MDR_out"));
                    break;
                case 1: //br_table;
                    //Get the opcode
                    opcode = opcode & IR;
                    opcode = opcode >> 9;

                    if(opcode == 7)
                    {
                        sentinel = true;
                    }
                    //Determine the CSAR
                    CSAR = dTable[opcode];
                    System.out.println(String.format("%-15s","br_table"));
                    break;
                case 1536: //MAR_in  IR_out;
                    MAR = addr & IR;
                    System.out.println(String.format("%-15s","MAR_in IR_out"));
                    break;
                case 8: //read;
                    MDR = mem[MAR];
                    System.out.println(String.format("%-15s","read"));
                    break;
                case 32896: //ACC_in  MDR_out;
                    ACC = MDR;
                    System.out.println(String.format("%-15s","ACC_in MDR_out"));
                    break;
                case 24576: //ACC_out alu_add;
                    TMP = (ACC + MDR) & 0xFFF;
                    System.out.println(String.format("%-15s","ACC_in alu_add"));
                    break;
                case 20480: //ACC_out ALU_sub
                    TMP = (ACC - MDR) & 0xFFF;
                    System.out.println(String.format("%-15s","ACC_out alu_sub"));
                    break;
                case 32772: //ACC_in  TMP_out;
                    ACC = TMP;
                    System.out.println(String.format("%-15s","ACC_in TMP_out"));
                    break;
                case 16640: //MDR_in  ACC_out;
                    MDR = ACC;
                    System.out.println(String.format("%-15s","MDR_in ACC_out"));
                    break;
                case 288: //PC-out MDR_in
                    MDR = PC;
                    System.out.println(String.format("%-15s","MDR_in PC_out"));
                    break;
                case 2: //write;
                    mem[MAR] = MDR;
                    System.out.println(String.format("%-15s","write"));
                    break;
                case 0: //or_addr;
                    if(ACC!=0){
                        CSAR = 0;
                    }
                    else{
                        CSAR = 1;
                    }
                    System.out.println(String.format("%-15s","or_addr"));
                    break;
            }

            //Print an instruction break
            if(CSAR == 0 && flagSum != 1)
            {
                System.out.println("     +---+---+---+---+---+---+/----//---------------------//---------------/");
            }
            //Increment the cycle counter
            m++;
        }

        //Print the sentinel
        String flags = "";
        for(int i = 0; i < 16; i++)
        {
            flags = flags + cs[CSAR][i];
        }
        System.out.println("     +---+---+---+---+---+---+/----//---------------------//---------------/");
        System.out.println("end of simulation");
        System.out.println("");

        //Print the first 20 memory values and formatting
        System.out.print("low memory:");
        for(int i = 0; i < 20; i++)
        {
            System.out.print(String.format("%5x", mem[i]));
            if(i == 9){
                System.out.println("");
                System.out.print(String.format("%11s"," "));
            }
        }
    }

}
