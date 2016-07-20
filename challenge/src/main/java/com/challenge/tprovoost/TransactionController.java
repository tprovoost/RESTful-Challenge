package com.challenge.tprovoost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest Controller used to handle all requests.
 * 
 * @author Thomas Provoost
 *
 */
@RestController
@RequestMapping("/transactionservice")
public class TransactionController {

	/**
	 * Map used to store all transactions in memory. ConcurrentHashMap is enough
	 * for multiple clients access.
	 */
	private Map<Long, Transaction> transactions = new ConcurrentHashMap<>();

	/**
	 * This is the method used for creating Transactions and add them into the
	 * memory. The Http request must follow this pattern : <br/>
	 * <code>PUT /transactionservice/transaction/$transaction_id</code><br/>
	 * <br/>
	 * Body:<br/>
	 * <br/>
	 * <code>{ "amount":double,"type":string,"parent_id":long }</code><br/>
	 * <br/>
	 * where:<br/>
	 * <br/>
	 * <b>transaction_id</b>: long specifying a new transaction<br/>
	 * <b>amount</b>: double specifying the amount<br/>
	 * <b>type</b>: string specifying a type of the transaction.<br/>
	 * <b>parent_id</b>: optional long that may specify the parent transaction
	 * of this transaction.
	 * 
	 * @param transactionId
	 *            : the id of the transaction. Used for the storage in the map.
	 * @param transaction
	 *            : the transaction to put in the map.
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

	/**
	 * Retrieves a transaction stored in memory through its ID. This method
	 * validates the ID first and throws a {@link TransactionNotFoundException}
	 * if it doesn't exist.<br/>
	 * <br/>
	 * <code>GET /transactionservice/transaction/$transaction_id</code>
	 * 
	 * @param transactionId
	 * @return The transaction :
	 *         <code>{ "amount":double,"type":string,"parent_id":long }</code>.
	 */
	@RequestMapping(value = "/transaction/{transactionId}", method = RequestMethod.GET)
	public ResponseEntity<Transaction> transactionGET(@PathVariable String transactionId) {

		Long id = this.validateId(transactionId);
		Transaction transaction = transactions.get(id);

		if (transaction == null)
			throw new TransactionNotFoundException(transactionId);

		return new ResponseEntity<Transaction>(transaction, HttpStatus.OK);
	}

	/**
	 * Retrieves a JSON array of IDs of all transactions of type
	 * <code>{type}</code>. This method validates the ID first and throws a
	 * {@link TransactionNotFoundException} if it doesn't exist.<br/>
	 * <br/>
	 * <code>GET /transactionservice/types/$type</code>
	 * 
	 * @param transactionId
	 * @return The transaction :
	 *         <code>{ "amount":double,"type":string,"parent_id":long }</code>.
	 */
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
	public static double sum(Long id, Map<Long, Transaction> transactions) {
		if (id == null)
			return 0;
		Transaction transaction = transactions.get(id);
		return transaction.getAmount() + sum(transaction.getParent_id(), transactions);
	}

	/**
	 * Validates an ID as a long.
	 * 
	 * @param transactionId
	 * @return
	 */
	private Long validateId(String transactionId) {
		try {
			return Long.valueOf(transactionId);
		} catch (IllegalArgumentException e) {
			throw new IllegalFormatException(transactionId);
		}
	}

	/**
	 * Validates a transaction. If the parent ID doesn't exist in the map,
	 * throws a {@link TransactionNotFoundException}.
	 * 
	 * @param transaction
	 */
	private void validateTransaction(Transaction transaction) {
		Long parent_id = transaction.getParent_id();
		if (parent_id != null && !transactions.containsKey(parent_id))
			throw new TransactionNotFoundException("" + parent_id);
	}

	/**
	 * Class used by the service to return a NOT_FOUND exception when a
	 * transaction was not found with a given ID.
	 * 
	 * @author Thomas Provoost
	 *
	 */
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

	/**
	 * Class used by the service to return a BAD_REQUEST exception when an ID is
	 * not valid.
	 * 
	 * @author Thomas Provoost
	 *
	 */
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
