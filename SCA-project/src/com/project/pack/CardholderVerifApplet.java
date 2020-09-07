/** 
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
 * 
 */


package com.project.pack;

import javacard.framework.*;
import javacardx.annotations.*;
import static com.project.pack.CardholderVerifAppletStrings.*;

/**
 * Applet class
 * 
 * @author <user>
 */
@StringPool(value = {
	    @StringDef(name = "Package", value = "com.project.pack"),
	    @StringDef(name = "AppletName", value = "CardholderVerifApplet")},
	    // Insert your strings here 
	name = "CardholderVerifAppletStrings")
public class CardholderVerifApplet extends Applet {

    /**
     * Installs this applet.
     * 
     * @param bArray
     *            the array containing installation parameters
     * @param bOffset
     *            the starting offset in bArray
     * @param bLength
     *            the length in bytes of the parameter data in bArray
     */
	
	  /* constants declaration */

    // code of CLA byte in the command APDU header
    final static byte Wallet_CLA = (byte) 0x80;

    // codes of INS byte in the command APDU header
 	final static byte WITHOUT_VERIF = (byte) 0x10;
 	final static byte PLAINTEXT_PIN_VERIF = (byte) 0x20;
 	final static byte CREDIT = (byte) 0x30;
 	final static byte DEBIT = (byte) 0x40;
 	final static byte GET_BALANCE = (byte) 0x50;
 	final static byte GET_CVM_LIST = (byte) 0x70;
 	
 	// maximum balance
    final static short MAX_BALANCE = 0x7FFF;
    // maximum transaction amount
    final static byte MAX_TRANSACTION_AMOUNT = 127;

    // maximum number of incorrect tries before the
    // PIN is blocked
    final static byte PIN_TRY_LIMIT = (byte) 0x03;
    // maximum size PIN
    final static byte MAX_PIN_SIZE = (byte) 0x08;

    // signal that the PIN verification failed
    final static short SW_VERIFICATION_FAILED = 0x6300;
    // signal the the PIN validation is required
    // for a credit or a debit transaction
    final static short SW_PIN_VERIFICATION_REQUIRED = 0x6301;
    // signal invalid transaction amount
    // amount > MAX_TRANSACTION_AMOUNT or amount < 0
    final static short SW_INVALID_TRANSACTION_AMOUNT = 0x6A83;

    // signal that the balance exceed the maximum
    final static short SW_EXCEED_MAXIMUM_BALANCE = 0x6A84;
    // signal the the balance becomes negative
    final static short SW_NEGATIVE_BALANCE = 0x6A85;
    
    final static short SW_WRONG_LENGTH=0x6A86;
    
    //codes of conditions for trnsactions
 	final static private byte COND_1_b1 = (byte) 0x1F; // amount <50 Lei, no CVM required
 	final static private byte COND_1_b2 = (byte) 0x06;
 	//byte1: 00011111 - no cvm required  byte2: 00000110 if transaction is under x value
 	
 	final static private byte COND_2_b1 = (byte) 0x01; // amount >50 Lei, verify the plaintext pin
 	final static private byte COND_2_b2 = (byte) 0x07;
 	///byte1 :00000001 plaintext pin verif req byte2: 00000111 if transaction is over x value
 	
 	final static private byte Threshold_Amount= (byte) 0x32;
 	
 	/* instance variables declaration */
    OwnerPIN pin;
    short balance;
    boolean isValidated;
    
    
    public static void install(byte[] bArray, short bOffset, byte bLength) {
    	//create a CardholderVerificationApplet instance
        new CardholderVerifApplet(bArray, bOffset, bLength);
    }

    /**
     * Only this class's install method should create the applet object.
     */
    protected CardholderVerifApplet(byte[] bArray, short bOffset, byte bLength) {
    	
    	 // It is good programming practice to allocate
        // all the memory that an applet needs during
        // its lifetime inside the constructor
        pin = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_SIZE);

        byte iLen = bArray[bOffset]; // aid length
        bOffset = (short) (bOffset + iLen + 1);
        byte cLen = bArray[bOffset]; // info length
        bOffset = (short) (bOffset + cLen + 1);
        byte aLen = bArray[bOffset]; // applet data length
       

        // The installation parameters contain the PIN
        // initialization value
        pin.update(bArray, (short) (bOffset + 1), aLen);
        register();
    }
    

    /**
     * Processes an incoming APDU.
     * 
     * @see APDU
     * @param apdu
     *            the incoming APDU
     */
    @Override
    public void process(APDU apdu) {
        //Insert your code here
    	
    	// APDU object carries a byte array (buffer) to
        // transfer incoming and outgoing APDU header
        // and data bytes between card and CAD

        // At this point, only the first header bytes
        // [CLA, INS, P1, P2, P3] are available in
        // the APDU buffer.
        // The interface javacard.framework.ISO7816
        // declares constants to denote the offset of
        // these bytes in the APDU buffer

        byte[] buffer = apdu.getBuffer();
        // check SELECT APDU command
        
        if (apdu.isISOInterindustryCLA()) {
            if (buffer[ISO7816.OFFSET_INS] == (byte) (0xA4)) {
                return;
            }
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }
        
        // verify the reset of commands have the
        // correct CLA byte, which specifies the
        // command structure
        if (buffer[ISO7816.OFFSET_CLA] != Wallet_CLA) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }
        switch (buffer[ISO7816.OFFSET_INS]) {
        case GET_BALANCE:
            getBalance(apdu);
            return;
        case CREDIT:
            credit(apdu);
            return;
        case DEBIT:
            debit(apdu);
            return;
        case GET_CVM_LIST:
        	get_cardholder_methods(apdu);
        	return;
        case WITHOUT_VERIF:
            without_verification(apdu);
            return;
        case PLAINTEXT_PIN_VERIF:
            plaintext_pin_verification(apdu);
            return;
        	
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
    }
    }
    
    private void get_cardholder_methods(APDU apdu) {
    	
    	byte[] buffer = apdu.getBuffer();
    	short le = apdu.setOutgoing();
    	if (le < 20) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}
    	apdu.setOutgoingLength((byte) 20);
    	
    	//x
    	buffer[0]=0;
    	buffer[1]=0;
    	buffer[2]=0;
    	buffer[3]=Threshold_Amount;
    	
    	//y
    	buffer[4]=0;
    	buffer[5]=0;
    	buffer[6]=0;
    	buffer[7]=0;
    	
    	//cvm code for rule1
    	buffer[8]=COND_1_b1;
    	
    	//cvm condition code rule1
    	buffer[9]=COND_1_b2;
    	
    	//x
    	buffer[10]=0;
    	buffer[11]=0;
    	buffer[12]=0;
    	buffer[13]=Threshold_Amount;
    	
    	//y
    	buffer[14]=0;
    	buffer[15]=0;
    	buffer[16]=0;
    	buffer[17]=0;
    	
    	//cvm code for rule2
    	buffer[18]=COND_2_b1;
    	
    	//cvm condition code rule2
    	buffer[19]=COND_2_b2;
    	
    	// send the 20-byte balance at the offset
        // 0 in the apdu buffer
        apdu.sendBytes((short) 0, (short) 20);

    	
	
	}

	private void credit(APDU apdu) {

        byte[] buffer = apdu.getBuffer();

        // Lc byte denotes the number of bytes in the
        // data field of the command APDU
        byte numBytes = buffer[ISO7816.OFFSET_LC];

        // indicate that this APDU has incoming data
        // and receive data starting from the offset
        // ISO7816.OFFSET_CDATA following the 5 header
        
        // bytes.
        byte byteRead = (byte) (apdu.setIncomingAndReceive());

        // it is an error if the number of data bytes
        // read does not match the number in Lc byte
        if ((numBytes != 1) || (byteRead != 1)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // get the credit amount
        byte creditAmount = buffer[ISO7816.OFFSET_CDATA];

        // check the credit amount
        if ((creditAmount > MAX_TRANSACTION_AMOUNT) || (creditAmount < 0)) {
            ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);
        }

        // check the new balance
        if ((short) (balance + creditAmount) > MAX_BALANCE) {
            ISOException.throwIt(SW_EXCEED_MAXIMUM_BALANCE);
        }

        // credit the amount
        balance = (short) (balance + creditAmount);

    } // end of deposit method
    
    private void getBalance(APDU apdu) {

        byte[] buffer = apdu.getBuffer();

        // inform system that the applet has finished
        // processing the command and the system should
        // now prepare to construct a response APDU
        // which contains data field
        short le = apdu.setOutgoing();

        if (le < 2) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // informs the CAD the actual number of bytes
        // returned
        apdu.setOutgoingLength((byte) 2);

        // move the balance data into the APDU buffer
        // starting at the offset 0
        buffer[0] = (byte) (balance >> 8);
        buffer[1] = (byte) (balance & 0xFF);

        // send the 2-byte balance at the offset
        // 0 in the apdu buffer
        apdu.sendBytes((short) 0, (short) 2);

    } // end of getBalance method
    
    private void debit(APDU apdu) {
    	
    	if (!isValidated) {
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		}

        byte[] buffer = apdu.getBuffer();

        byte numBytes = (buffer[ISO7816.OFFSET_LC]);

        byte byteRead = (byte) (apdu.setIncomingAndReceive());

        if ((numBytes != 1) || (byteRead != 1)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // get debit amount
        byte debitAmount = buffer[ISO7816.OFFSET_CDATA];

        // check debit amount
        if ((debitAmount > MAX_TRANSACTION_AMOUNT) || (debitAmount < 0)) {
            ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);
        }

        // check the new balance
        if ((short) (balance - debitAmount) < (short) 0) {
            ISOException.throwIt(SW_NEGATIVE_BALANCE);
        }

        balance = (short) (balance - debitAmount);
        
        isValidated=false;

    } // end of debit method
    
    private void without_verification(APDU apdu) {
		isValidated = true;
	}
    
    private void plaintext_pin_verification(APDU apdu) {
    	
    	  byte[] buffer = apdu.getBuffer();
          // retrieve the PIN data for validation.
          byte byteRead = (byte) (apdu.setIncomingAndReceive());

          // check pin
          // the PIN data is read into the APDU buffer
          // at the offset ISO7816.OFFSET_CDATA
          // the PIN data length = byteRead
          if (pin.check(buffer, ISO7816.OFFSET_CDATA, byteRead) == false) {
              ISOException.throwIt(SW_VERIFICATION_FAILED);
          }
          
          isValidated=true;
    }
    


}
