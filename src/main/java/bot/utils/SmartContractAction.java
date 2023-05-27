package bot.utils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGasPrice;

import bot.constant.DEXConstant;

public class SmartContractAction {
    private static final Logger logger = LogManager.getLogger(SmartContractAction.class);
    
    private static final String[] send_errors = {"max fee per gas","insufficient funds for gas","intrinsic gas too low"};
    
    public BigInteger getBalanceInUsdc(Web3j web3j, Credentials credentials) throws IOException {
    	Function balanceOfFunction = new Function(
                "balanceOf",
                Collections.singletonList(new org.web3j.abi.datatypes.Address(credentials.getAddress())),
                Collections.singletonList(new TypeReference<Uint256>() {
                })
        );
    	
    	String encodedFunction = FunctionEncoder.encode(balanceOfFunction);
        org.web3j.protocol.core.methods.response.EthCall ethCall = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                		credentials.getAddress(), DEXConstant.USDC_ADDRESS, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .send();

        List<TypeReference<Type>> outputParameters = balanceOfFunction.getOutputParameters();
        List<Type> values = FunctionReturnDecoder.decode(ethCall.getValue(), outputParameters);
        BigInteger balance = (BigInteger) values.get(0).getValue();
        return balance;
    }

    private BigInteger getCurrentGasPrice(Web3j web3j) throws InterruptedException, ExecutionException, IOException {
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger currentGasPrice = ethGasPrice.getGasPrice();
        return currentGasPrice;
    }
    
    private boolean isNotEnoughBalance(Web3j web3j, Credentials credentials, Uint256 amountIn) throws IOException {
		BigInteger balanceInUsdc = getBalanceInUsdc(web3j, credentials);
		if (balanceInUsdc.compareTo(amountIn.getValue()) < 0) {
			logger.info("currentBalance is not enough: " + balanceInUsdc);
			return true;
		} else {
			return false;
		}
    }
}
