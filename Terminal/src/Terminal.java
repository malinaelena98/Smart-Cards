
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

import com.sun.javacard.apduio.Apdu;
import com.sun.javacard.apduio.CadClientInterface;
import com.sun.javacard.apduio.CadDevice;
import com.sun.javacard.apduio.CadTransportException;


public class Terminal {
	
	final static byte GET_BALANCE = (byte) 0x50;
	final static byte CREDIT = (byte) 0x30;

	private static Socket sock;
	private static OutputStream os;
	private static InputStream is;
	private static CadClientInterface cad;
	Apdu apdu;
	static File file = new File("C:\\Users\\Home\\SCAproject\\SCA-project\\apdu_scripts\\cap-SCA-project.script");

	private void connect() {
		try {
			sock = new Socket("localhost", 9025);
			os = sock.getOutputStream();
			is = sock.getInputStream();
			// Initialize the instance card acceptance device
			cad = CadDevice.getCadClientInstance(CadDevice.PROTOCOL_T1, is, os);

		} catch (IOException e) {
			System.err.println("Conection Fail!");
		}
	}

	public static void powerUp() throws IOException, CadTransportException {

		cad.powerUp();
		System.out.println("Card is on!");
	}

	public static void powerDown() throws IOException, CadTransportException {

		cad.powerDown();
		System.out.println("Card is off!");
	}

	public static void processCapFile() throws IOException, CadTransportException {
		// parse the cap file to install the applet
		Scanner file_s = new Scanner(file);
		//header
		byte[] byte_arr = new byte[4];
		String line;
		while (file_s.hasNextLine()) {
			line = file_s.nextLine();
			if (line.length() > 3) {
				line = line.substring(0, line.length() - 1);
				// have to get first line without // and !-powerup;
				if (line.charAt(0) != '/' && line.charAt(0) != 'p') {
					String[] splits = line.split(" ");

					// get the header
					for (int i = 0; i < 4; i++) {
						byte b = 0;

						b += Integer.parseInt(String.valueOf(splits[i].charAt(2)), 16) * 16;
						b += Integer.parseInt(String.valueOf(splits[i].charAt(3)), 16);

						byte_arr[i] = (byte) b;
					}

					int bodySize = splits.length - 6;
					byte[] body = new byte[bodySize];

					int j = 0;
					// get the body
					for (int i = 5; i < splits.length - 1; i++) {

						byte b = 0;
						b += Integer.parseInt(String.valueOf(splits[i].charAt(2)), 16) * 16;
						b += Integer.parseInt(String.valueOf(splits[i].charAt(3)), 16);

						body[j] = (Byte) b;
						j++;
					}
					Apdu apdu = new Apdu();
					apdu.command = byte_arr;
					apdu.setDataIn(body);
				
					cad.exchangeApdu(apdu);
//				if(apdu.getStatus()==0x9000) {
//					System.out.println("succes");
//				}else {
//					System.out.println("Error: "+ apdu.getStatus());
//				}
				}
			}
		}

	}

	private static void create() throws IOException, CadTransportException {
		Apdu apdu = new Apdu();
		apdu.command[0] = (byte) 0x80;
		apdu.command[1] = (byte) 0xB8;
		apdu.command[2] = (byte) 0x00;
		apdu.command[3] = (byte) 0x00;
		apdu.Lc = (byte) 0x14;
		byte[] data = new byte[] { (byte) 0x0a, (byte) 0xa0, 0x0, 0x0, 0x0, 0x62, 0x3, 0x1, 0xc, 0x6, 0x1, 0x08, 0x0,
				0x0, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05 };

		apdu.dataIn = data;
		apdu.setDataIn(apdu.dataIn, apdu.Lc);
		
		try {
			cad.exchangeApdu(apdu);
			// System.out.println(apdu);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CadTransportException e) {
			e.printStackTrace();
		}
		if (apdu.getStatus() != 0x9000) {
			System.out.println("Applet creation error -> " + apdu.getStatus());
			System.exit(1);
		}
	}

	private void select() {

		apdu = new Apdu();
		apdu.command[Apdu.CLA] = 0x00;
		apdu.command[Apdu.INS] = (byte) 0xA4;
		apdu.command[Apdu.P1] = 0x04;
		apdu.command[Apdu.P2] = 0x00;
		apdu.setDataIn(new byte[] { (byte) 0xa0, 0x0, 0x0, 0x0, 0x62, 0x3, 0x1, 0xc, 0x6, 0x1 });
		try {
			cad.exchangeApdu(apdu);
			//System.out.println(apdu);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CadTransportException e) {
			e.printStackTrace();
		}
		if (apdu.getStatus() != 0x9000) {
			System.out.println("Applet selection error -> " + apdu.getStatus());
			System.exit(1);
		}
	}

	private String get_cvm_list() {

		apdu = new Apdu();
		apdu.command = new byte[] { (byte) 0x80, (byte) 0x70, 0x00, 0x00 };
		//apdu.setDataIn(new byte[] {});
		//expcted no of bytes
		apdu.setLe(0x14);
		try {
			cad.exchangeApdu(apdu);
			//System.out.println(apdu);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CadTransportException e) {
			e.printStackTrace();
		}
		if (apdu.getStatus() != 0x9000) {
			System.out.println("error in getting cvm list -> " + apdu.getStatus());
			System.exit(1);
		}

		return apdu.toString();
	}

	private void no_pin_verification() throws IOException {

		apdu = new Apdu();
		apdu.command[Apdu.CLA] = (byte) 0x80;
		apdu.command[Apdu.INS] = (byte) 0x10;
		apdu.command[Apdu.P1] = 0x00;
		apdu.command[Apdu.P2] = 0x00;
		try {
			cad.exchangeApdu(apdu);
			//System.out.println(apdu);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CadTransportException e) {
			e.printStackTrace();
		}
		if (apdu.getStatus() != 0x9000) {
			System.out.println("PIN verification failed." + " -> " + apdu.getStatus());
		}
		else {
				System.out.println("Amount < 50 lei, you don't have to insert PIN!");
			}
		

	}

	private void verify_pin() throws IOException {
		byte[] PIN;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Insert PIN: ");
		String pin = br.readLine();
		PIN = convert_str_to_byte(pin);

		apdu = new Apdu();
		apdu.command[Apdu.CLA] = (byte) 0x80;
		apdu.command[Apdu.INS] = (byte) 0x20;
		apdu.command[Apdu.P1] = 0x00;
		apdu.command[Apdu.P2] = 0x00;
		apdu.Lc = (byte) 0x05;

		 apdu.setDataIn(PIN);
		

		try {
			cad.exchangeApdu(apdu);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CadTransportException e) {
			e.printStackTrace();
		}
		if (apdu.getStatus() == 0x9000) {
			System.out.println("Correct pin!");
			
		}
		else if(apdu.getStatus()==0x6300) {
			System.out.println("Verification failed -> " + "SW_VERIFICATION_FAILED");
			
		}
		else {
			System.out.println("Verification failed -> " + apdu.getStatus());
		}
	}

	//convert string to byte -> used to send the pin
	public static byte[] convert_str_to_byte(String code) {
		byte[] result = new byte[code.length()];
		for (int i = 0; i < code.length(); i++) {
			int number = Character.getNumericValue(code.charAt(i));
			result[i] = (byte) (number);
		}
		return result;
	}

	public void credit() throws IOException {

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Credit amount: ");
		short amount;
		try {
			amount = Short.parseShort(br.readLine());
		} catch (NumberFormatException e) {
			throw new IOException("Amount is invalid");
		}

		byte byteAmount = (byte) (amount & 0xff);

		// System.out.println(byteAmount);
		apdu = new Apdu();
		apdu.command[Apdu.CLA] = (byte) 0x80;
		apdu.command[Apdu.INS] = (byte) 0x30;
		apdu.command[Apdu.P1] = 0x00;
		apdu.command[Apdu.P2] = 0x00;
		apdu.setDataIn(new byte[] { byteAmount }, 0x01);
		apdu.setLe(0x7f);

		try {
			cad.exchangeApdu(apdu);
			//System.out.println(apdu);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CadTransportException e) {
			e.printStackTrace();
		}
		
		 if(apdu.getStatus()==0x6A83) {
			System.out.println("The attempt to credit " + byteAmount + " failed!"+ " ->SW_INVALID_TRANSACTION_AMOUNT" );
		}
		else if(apdu.getStatus() == 0x9000) {
			System.out.println("The attempt to credit " + byteAmount + " succeeded!");
		}
		else
		{
			System.out.println("the attempt to credit failed." + " -> " + apdu.getStatus());
		}

	}
	
	public void getBalance() {

		apdu = new Apdu();
		apdu.command[Apdu.CLA] = (byte) 0x80;
		apdu.command[Apdu.INS] = (byte) 0x50;
		apdu.command[Apdu.P1] = 0x00;
		apdu.command[Apdu.P2] = 0x00;
		try {
			cad.exchangeApdu(apdu);
			//System.out.println(apdu);
			System.out.println("The attempt to get account balance succeeded!");
			String balance=apdu.toString();
			int start_index = balance.indexOf("Le: ");
			String l = balance.substring(start_index + 8, start_index + 14);
			String[] splits = l.split(", ");
			String byte1=Arrays.asList(splits).get(0);
			String byte2=Arrays.asList(splits).get(1);
			System.out.println("Yout current balance is: "+Integer.decode("0x"+byte1+byte2)+" lei");

			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CadTransportException e) {
			e.printStackTrace();
		}
		if (apdu.getStatus() != 0x9000) {
			System.out.println("the attempt to get account balance failed ." + " -> " + apdu.getStatus());
			
		}
	}

	public void debit() throws IOException, CadTransportException {

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("debit amount: ");
		String amount = br.readLine();
		//get cvm list
		String list = get_cvm_list();
		//identify Le
		int start_index = list.indexOf("Le:");
		String l = list.substring(start_index + 8, start_index + 86);
		String[] splits = l.split(", ");
		String threshold_amount = Arrays.asList(splits).get(3);
		String Rule1=Arrays.asList(splits).get(8)+Arrays.asList(splits).get(9);
		String Rule2=Arrays.asList(splits).get(18)+Arrays.asList(splits).get(19);
		String rule_to_apply;

		String str_to_decode = "0x" + threshold_amount;

		// case 1-> if the debit amount < our threshold
		// in this case, we don t need to verify pin
		if (Integer.parseInt(amount) < Integer.decode(str_to_decode)) {
			rule_to_apply=Rule1;
		} else {
			rule_to_apply=Rule2;
		}

		switch (rule_to_apply) {
		case "1f06":
			no_pin_verification();
			break;

		case "0107":
			verify_pin();
			break;

		default:
			System.out.println("Unrecognized command");

		}
		short amount_to_send;

		try {
			amount_to_send = Short.parseShort(amount);
		} catch (NumberFormatException e) {
			throw new IOException("Amount is invalid");
		}

		byte byteAmount = (byte) (amount_to_send & 0xff);

		apdu = new Apdu();
		apdu.command[Apdu.CLA] = (byte) 0x80;
		apdu.command[Apdu.INS] = (byte) 0x40;
		apdu.command[Apdu.P1] = 0x00;
		apdu.command[Apdu.P2] = 0x00;
		apdu.setDataIn(new byte[] { byteAmount }, 0x01);
		apdu.setLe(0x7f);
		try {
			cad.exchangeApdu(apdu);
			//System.out.println(apdu);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CadTransportException e) {
			e.printStackTrace();
		}
		
		if(apdu.getStatus()==0x9000) {
			System.out.println("The attempt to debit " + byteAmount + " succeeded!");
		}
		else if(apdu.getStatus()==0x6A85) {
			System.out.println("The attempt to debit " + byteAmount + " failed!"+ " ->SW_NEGATIVE_BALANCE" );

		}
		else if(apdu.getStatus()==0x6A83) {
			System.out.println("The attempt to debit " + byteAmount + " failed!"+ " ->SW_INVALID_TRANSACTION_AMOUNT" );
		}
		else {
			System.out.println("the attempt to debit failed." + " -> " + apdu.getStatus());
		}

	}

	public static void main(String[] argv) throws Exception {
		Terminal terminal = new Terminal();
		terminal.connect();
		terminal.powerUp();
		terminal.processCapFile();
		terminal.create();
		terminal.select();
		
		while(true) {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Insert your operation: credit/debit/balance/close ");
		String op = br.readLine().toLowerCase();
		switch (op){
		case("credit"):
			terminal.credit();
			break;
		case("debit"):
			terminal.debit();
			break;
		case("balance"):
			terminal.getBalance();
			break;
		case("close"):
			terminal.powerDown();
		sock.close();
		System.exit(0);
		
		default:
            System.out.println("Invalid command: " + op);
            continue;
			
			
		}
		
		}

	}

}