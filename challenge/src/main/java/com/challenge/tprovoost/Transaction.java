package com.challenge.tprovoost;

public class Transaction {

	private Double amount;
	private String type;
	private Long parent_id;

	public Transaction() {
	}

	public Transaction(Double amount, String type) {
		this(amount, type, null);
	}

	public Transaction(Double amount, String type, Long parent_id) {
		this.amount = amount;
		this.type = type;
		this.parent_id = parent_id;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Long getParent_id() {
		return parent_id;
	}

	public void setParent_id(Long parent_id) {
		this.parent_id = parent_id;
	}

	@Override
	public String toString() {
		return "Transaction [amount=" + amount + ", type=" + type + ", parent_id=" + parent_id + "]";
	}
	
}
