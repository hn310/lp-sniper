package bot.model;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint24;

public class LPModel {
	private Address token0;
	private Address token1;
	private Uint24 fee;
	private Address poolAddress;
	public Address getToken0() {
		return token0;
	}
	public void setToken0(Address token0) {
		this.token0 = token0;
	}
	public Address getToken1() {
		return token1;
	}
	public void setToken1(Address token1) {
		this.token1 = token1;
	}
	public Uint24 getFee() {
		return fee;
	}
	public void setFee(Uint24 fee) {
		this.fee = fee;
	}
	public Address getPoolAddress() {
		return poolAddress;
	}
	public void setPoolAddress(Address poolAddress) {
		this.poolAddress = poolAddress;
	}
	
	@Override
	public String toString() {
		return "token0: " + this.token0 + 
				", token1: " + this.token1 + 
				", fee: " + this.fee.getValue() +
				", poolAddress: " + this.poolAddress;
	}
}
