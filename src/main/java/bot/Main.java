package bot;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;

import bot.constant.AccConstant;
import bot.constant.RPCConstant;
import bot.utils.SmartContractAction;

public class Main {
	private static final Logger logger = LogManager.getLogger(Main.class);
	
	private static Credentials credentials = null;

	public static void main(String[] args)
			throws InterruptedException, ExecutionException, IOException, TransactionException {

		// Connect to Ethereum client using web3j
		Web3j web3j = Web3j.build(new HttpService(RPCConstant.ARBITRUM_ONE_RPC));
		credentials = Credentials.create(AccConstant.SNIPER_KEY);

		// start with the latest block whenever start program to avoid old trades
//		BigInteger latestBlock = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock()
//				.getNumber();
//		new BlockchainAction().writeLastBlockNo(latestBlock.intValueExact());
		
		SmartContractAction sc = new SmartContractAction();
		sc.getLPList(web3j);
	}
}
