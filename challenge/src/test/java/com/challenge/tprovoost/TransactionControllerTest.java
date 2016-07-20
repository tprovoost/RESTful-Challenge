package com.challenge.tprovoost;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * Class under test : {@link TransactionController}.
 * 
 * @author Thomas Provoost
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class TransactionControllerTest {

	private static final Long T1_ID = 10L;
	private static final double T1_AMOUNT = 5000;
	private static final String T1_TYPE = "cars";

	private static final Long T2_ID = 11L;
	private static final double T2_AMOUNT = 10000;
	private static final String T2_TYPE = "shopping";

	@Autowired
	private WebApplicationContext ctx;

	/** Spring Mock MVC used to contact the mock server. */
	private MockMvc mockMvc;

	/** The JSON content type of HTTP requests. */
	private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

	/** HTTP Message Converter to map the JSON. */
	private HttpMessageConverter<Object> mappingJackson2HttpMessageConverter;

	@Autowired
	void setConverters(HttpMessageConverter<Object>[] converters) {
		this.mappingJackson2HttpMessageConverter = Arrays.asList(converters)
				.stream()
				.filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
				.findAny()
				.get();

		assertNotNull("the JSON message converter must not be null", this.mappingJackson2HttpMessageConverter);
	}

	@Before
	public void setUp() {
		this.mockMvc = webAppContextSetup(ctx).build();
	}

	/**
	 * Method under test :
	 * {@link TransactionController#sum(Long, LinkedHashMap)}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSum() throws Exception {
		HashMap<Long, Transaction> transactions = new HashMap<>();
		transactions.put(T1_ID, new Transaction(T1_AMOUNT, T1_TYPE));
		transactions.put(T2_ID, new Transaction(T2_AMOUNT, T2_TYPE, T1_ID));

		double expectedSum10 = 5000;
		double expectedSum11 = 15000;

		assertEquals(expectedSum10, TransactionController.sum(T1_ID, transactions), 0);
		assertEquals(expectedSum11, TransactionController.sum(11L, transactions), 0);
	}

	/**
	 * Case : Single transaction created.
	 * 
	 * @throws Exception
	 * @see {@link TransactionController#transactionPUT(String, Transaction)}
	 */
	@Test
	public void createTransaction() throws Exception {
		String bookmarkJson = json(new Transaction(T1_AMOUNT, T1_TYPE));
		this.mockMvc.perform(put("/transactionservice/transaction/" + T1_ID)
				.contentType(contentType)
				.content(bookmarkJson))
				.andExpect(status().isOk());
	}

	/**
	 * Case : Two transactions created.
	 * 
	 * @throws Exception
	 * @see {@link TransactionController#transactionPUT(String, Transaction)}
	 */
	@Test
	public void createTransactions() throws Exception {
		String bookmarkJson = json(new Transaction(T1_AMOUNT, T1_TYPE));
		this.mockMvc.perform(put("/transactionservice/transaction/" + T1_ID)
				.contentType(contentType)
				.content(bookmarkJson))
				.andExpect(status().isOk());

		bookmarkJson = json(new Transaction(T2_AMOUNT, T2_TYPE, T1_ID));
		this.mockMvc.perform(put("/transactionservice/transaction/" + T2_ID)
				.contentType(contentType)
				.content(bookmarkJson))
				.andExpect(status().isOk());
	}

	/**
	 * Populates the server with two transactions, then reads the first one.
	 * 
	 * @throws Exception
	 * @see {@link TransactionController#transactionGET(String)}
	 */
	@Test
	public void readSingleTransaction() throws Exception {
		createTransactions();
		mockMvc.perform(get("/transactionservice/transaction/" + T1_ID))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(contentType))
				.andExpect(jsonPath("$.amount",
						is(T1_AMOUNT)))
				.andExpect(jsonPath("$.type", is("cars")))
				.andExpect(jsonPath("$.parent_id", IsNull.nullValue()));
	}

	/**
	 * Populates the server with two transactions, then reads an unknown one.
	 * Expecting a NOT_FOUND result.
	 * 
	 * @throws Exception
	 * @see {@link TransactionController#transactionGET(String)}
	 */
	@Test
	public void transactionNotFound() throws Exception {
		createTransactions();
		mockMvc.perform(get("/transactionservice/transaction/0")
				.contentType(contentType))
				.andExpect(status().isNotFound());
	}

	/**
	 * Populates the server with two transactions, then get the list of all
	 * transactions of type {@link #T1_TYPE}.
	 * 
	 * @throws Exception
	 * @see {@link TransactionController#type(String)}
	 */
	@Test
	public void readType() throws Exception {
		createTransactions();
		mockMvc.perform(get("/transactionservice/types/" + T1_TYPE))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(contentType))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0]", is(T1_ID.intValue())));
	}

	/**
	 * Populates the server with two transactions, then get the list of all
	 * transactions of an unknown type. Expecting an empty list.
	 * 
	 * @throws Exception
	 * @see {@link TransactionController#type(String)}
	 */
	@Test
	public void readUnknownType() throws Exception {
		createTransactions();
		mockMvc.perform(get("/transactionservice/types/unknownType"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(contentType))
				.andExpect(jsonPath("$", hasSize(0)));
	}

	/**
	 * Populates the server with two transactions, then call the sum method on
	 * {@link #T1_ID}.
	 * 
	 * @throws Exception
	 * @see {@link TransactionController#sum(String)}
	 */
	@Test
	public void readSum() throws Exception {
		createTransactions();
		mockMvc.perform(get("/transactionservice/sum/" + T1_ID))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().string("{\"sum\":5000.0}"));
	}

	/**
	 * This method uses a {@link HttpMessageConverter} to convert an object into
	 * a JSON string through a {@link MockHttpOutputMessage}.
	 * 
	 * @param o
	 *            The object to transform into JSON.
	 * @return A String containing the JSON representation of o.
	 * @throws IOException
	 */
	protected String json(Object o) throws IOException {
		MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
		mappingJackson2HttpMessageConverter.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
		return mockHttpOutputMessage.getBodyAsString();
	}
}
