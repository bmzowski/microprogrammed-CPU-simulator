# microprogrammed-CPU-simulator
Simulation of a microprogrammed CPU with extensions.

Goal: write microinstructions for the follow instruction set and include them in the control store for the simulator.

Instruction set includes:

load  -- opcode = 000 -- acc <- mem[addr]  
add   -- opcode = 001 -- acc <- acc + mem[addr]  
store -- opcode = 010 -- mem[addr] <- acc  
brz   -- opcode = 011 -- if( acc==0 ) pc <- addr  
sub   -- opcode = 100 -- acc <- acc - mem[addr]  
jsub  -- opcode = 101 -- mem[addr] <- updated pc; pc <- addr + 1  
jmpi  -- opcode = 110 -- pc <- mem[addr]  
halt  -- opcode = 111 -- halt  

The simulator reads a list of memory words in hex, ending with a sentinel of -1, from an input file named "microsim.txt".
For output it displays all six control and datapath registers, the CSAR and CSIR, and the contents of the first 20 words in memory.

Instructions are 12 bits, a 3-bit opcode followed by a 9-bit address.  

Example instruction:    
|010|000001001|  

 where bits 0-2  == 3-bit opcode  
 bits 3-11 == 9-bit address field  
 
 or, given as three hex digits  
 memory-word 0x409  

(The example is "store 9", that is, store a copy of the value in the accumulator into the 12-bit memory word at location 9.)
