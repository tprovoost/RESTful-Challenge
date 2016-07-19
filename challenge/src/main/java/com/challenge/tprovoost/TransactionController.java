package com.challenge.tprovoost;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactionservice")
public class TransactionController {

	private LinkedHashMap<Long, Transaction> transactions = new LinkedHashMap<>();

	/**
	 * This is the method used for creating Transactions and add them into the
	 * memory.
	 * 
	 * @param transactionId
	 *            : the id of the transaction.
	 * @param transaction
	 * @return
	 */
	@RequestMapping(value = "/transaction/{transactionId}", method = RequestMethod.PUT)
	public ResponseEntity<Transaction> transactionPUT(@PathVariable String transactionId,
			@RequestBody Transaction transaction) {

		this.validateTransaction(transaction);
		Long id = this.validateId(transactionId);
		transactions.put(id, transaction);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/transaction/{transactionId}", method = RequestMethod.GET)
	public ResponseEntity<Transaction> transactionGET(@PathVariable String transactionId) {

		Long id = this.validateId(transactionId);
		Transaction transaction = transactions.get(id);

		if (transaction == null)
			throw new TransactionNotFoundException(transactionId);

		return new ResponseEntity<Transaction>(transaction, HttpStatus.OK);
	}

	@RequestMapping(value = "/types/{type}", method = RequestMethod.GET)
	public ResponseEntity<List<Long>> type(@PathVariable String type) {

		ArrayList<Long> types = new ArrayList<>();

		transactions.forEach((id, t) -> {
			if (t.getType().contentEquals(type))
				types.add(id);
		});

		return new ResponseEntity<List<Long>>(types, HttpStatus.OK);
	}

	/**
	 * Method responding to "/transactionservice/sum/{transaction_id}" for verb
	 * GET. It will sums the amounts contained in all transactions from the
	 * child transaction up to his top-level parent.
	 * 
	 * @param transactionId
	 *            : the desired transaction from which the sums starts.
	 * @return A {@link ResponseEntity} containing the value.
	 * 
	 * @see #sum(int)
	 */
	@RequestMapping(value = "/sum/{transactionId}", method = RequestMethod.GET)
	public ResponseEntity<String> sum(@PathVariable String transactionId) {

		Long id = this.validateId(transactionId);
		String data = "{\"sum\":" + sum(id, transactions) + "}";
		return new ResponseEntity<String>(data, HttpStatus.OK);
	}

	/**
	 * Recursive methods that sums all object from a transaction child to his
	 * top parent.
	 * 
	 * @param id
	 *            : id of the transaction
	 * @return Returns the current sum.
	 */
	public static double sum(Long id, LinkedHashMap<Long, Transaction> transactions) {
		if (id == null)
			return 0;
		Transaction transaction = transactions.get(id);
		return transaction.getAmount() + sum(transaction.getParent_id(), transactions);
	}

	private Long validateId(String transactionId) {
		try {
			return Long.valueOf(transactionId);
		} catch (IllegalArgumentException e) {
			throw new IllegalFormatException(transactionId);
		}
	}

	private void validateTransaction(Transaction transaction) {
		Long parent_id = transaction.getParent_id();
		if (parent_id != null && !transactions.containsKey(parent_id))
			throw new TransactionNotFoundException("" + parent_id);
	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	class TransactionNotFoundException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6689057492574855137L;

		public TransactionNotFoundException(String transactionId) {
			super("could not find transaction '" + transactionId + "'.");
		}
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	class IllegalFormatException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -1821498446165102695L;

		public IllegalFormatException(String transactionId) {
			super("the following transaction_id : '" + transactionId + "' is not of type 'long'.");
		}
	}

}
