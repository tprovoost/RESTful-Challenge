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

import com.challenge.tprovoost.Application;
import com.challenge.tprovoost.Transaction;
import com.challenge.tprovoost.TransactionController;

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

	/** The content type of JSON requests. */
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

	@Test
	public void testSum() throws Exception {
		LinkedHashMap<Long, Transaction> transactions = new LinkedHashMap<>();
		transactions.put(T1_ID, new Transaction(T1_AMOUNT, T1_TYPE));
		transactions.put(T2_ID, new Transaction(T2_AMOUNT, T2_TYPE, T1_ID));

		double expectedSum10 = 5000;
		double expectedSum11 = 15000;

		assertEquals(expectedSum10, TransactionController.sum(T1_ID, transactions), 0);
		assertEquals(expectedSum11, TransactionController.sum(11L, transactions), 0);
	}

	@Test
	public void createTransaction() throws Exception {
		String bookmarkJson = json(new Transaction(5000d, "cars"));
		this.mockMvc.perform(put("/transactionservice/transaction/" + T1_ID)
				.contentType(contentType)
				.content(bookmarkJson))
				.andExpect(status().isOk());
	}

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

	@Test
	public void transactionNotFound() throws Exception {
		mockMvc.perform(get("/transactionservice/transaction/0")
				.contentType(contentType))
				.andExpect(status().isNotFound());
	}

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

	@Test
	public void readUnknownType() throws Exception {
		createTransactions();
		mockMvc.perform(get("/transactionservice/types/unknownType"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(contentType))
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	public void readSum() throws Exception {
		createTransactions();
		mockMvc.perform(get("/transactionservice/sum/" + T1_ID))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().string("{\"sum\":5000.0}"));
	}

	protected String json(Object o) throws IOException {
		MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
		mappingJackson2HttpMessageConverter.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
		return mockHttpOutputMessage.getBodyAsString();
	}
}
