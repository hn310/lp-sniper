package bot.utils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Int24;
import org.web3j.abi.datatypes.generated.Uint24;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthLog.LogResult;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import bot.constant.DEXConstant;
import bot.model.LPModel;

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
    
    public void getLPList(Web3j web3j) throws IOException {
    	// Event signature for PoolCreated event in Uniswap v3
    	List<TypeReference<?>> parameters = new ArrayList<TypeReference<?>>();
    	parameters.add(new TypeReference<Address>() {}); // token 0
    	parameters.add(new TypeReference<Address>() {}); // token 1
    	parameters.add(new TypeReference<Uint24>() {}); // fee
    	parameters.add(new TypeReference<Int24>() {}); // tickSpacing
    	parameters.add(new TypeReference<Address>() {}); // pool
    	Event event = new Event("PoolCreated", parameters);
    	
        // Create the EthFilter
        EthFilter filter = new EthFilter(
        		new DefaultBlockParameterNumber(BigInteger.valueOf(95177616)),
                DefaultBlockParameterName.LATEST,
                DEXConstant.UNISWAP_V3_FACTORY
        );
        filter.addSingleTopic(EventEncoder.encode(event));

        EthLog ethLog = web3j.ethGetLogs(filter).send();

        List<LogResult> logResults = ethLog.getLogs();
        for (LogResult logResult : logResults) {
            if (logResult instanceof EthLog.LogObject) {
            	LPModel lpModel = new LPModel();
                EthLog.LogObject logObject = (EthLog.LogObject) logResult;
                // Process the log object
                setPoolCreatedInfo(logObject.get(), lpModel);
                System.out.println(lpModel);
            }
        }
    }
    
    private void setPoolCreatedInfo(Log log, LPModel lpModel) {
    	// IndexedParameters
        // Extract the addresses of the token pair from the event log
        String tokenAAddressInHex = log.getTopics().get(1);
        String tokenBAddressInHex = log.getTopics().get(2);
        String feeInHex = log.getTopics().get(3);
        Address token0 = new Address(tokenAAddressInHex);
        Address token1 = new Address(tokenBAddressInHex);
        Uint24 fee = new Uint24(Numeric.toBigInt(feeInHex));
        
        lpModel.setToken0(token0);
        lpModel.setToken1(token1);
        lpModel.setFee(fee);
        
        // NonIndexedParameters
        List<TypeReference<?>> parameters = new ArrayList<TypeReference<?>>();
    	parameters.add(new TypeReference<Int24>() {}); // tickSpacing
    	parameters.add(new TypeReference<Address>() {}); // pool
    	List<Type> decodedData = FunctionReturnDecoder.decode(log.getData(), Utils.convert(parameters));

    	Int24 tickSpacing = (Int24) decodedData.get(0); // tickSpacing
    	Address pool = (Address) decodedData.get(1); // pool
    	lpModel.setTickSpacing(tickSpacing);
    	lpModel.setPoolAddress(pool);
    }
    
    private void getTokenAmountInLP() {
    	
    }
    
    // SAMPLE ONLY
    private LPModel getPoolAddress(Web3j web3j, LPModel lpModel) throws IOException {
        List<Type> inputs = new ArrayList<Type>();
        List<TypeReference<?>> outputs = new ArrayList<TypeReference<?>>();

        // set inputs
        inputs.add(lpModel.getToken0());
        inputs.add(lpModel.getToken1());
        inputs.add(lpModel.getFee());

        // set outputs
        outputs.add(new TypeReference<Address>() {}); // pool address

        // call function
        Function function = new Function("getPool", // Function name
                inputs, outputs); // Function returned parameters

        String encodedFunction = FunctionEncoder.encode(function);
        EthCall encodedResponse = web3j.ethCall(Transaction.createEthCallTransaction(null, DEXConstant.UNISWAP_V3_FACTORY, encodedFunction), DefaultBlockParameterName.LATEST).send();

        List<Type> response = FunctionReturnDecoder.decode(encodedResponse.getValue(), function.getOutputParameters());
        if (response.size() > 0) {
            lpModel.setPoolAddress(new Address(response.get(0).getValue().toString()));
        }
        
        return lpModel;
    }
}
