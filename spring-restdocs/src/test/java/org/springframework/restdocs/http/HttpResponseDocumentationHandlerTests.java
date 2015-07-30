/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.restdocs.http;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.restdocs.snippet.Attributes.attributes;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.restdocs.test.SnippetMatchers.httpResponse;
import static org.springframework.restdocs.test.StubMvcResult.result;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.restdocs.templates.TemplateEngine;
import org.springframework.restdocs.templates.TemplateResourceResolver;
import org.springframework.restdocs.templates.mustache.MustacheTemplateEngine;
import org.springframework.restdocs.test.ExpectedSnippet;

/**
 * Tests for {@link HttpResponseSnippet}
 *
 * @author Andy Wilkinson
 * @author Jonathan Pearlin
 */
public class HttpResponseDocumentationHandlerTests {

	@Rule
	public final ExpectedSnippet snippet = new ExpectedSnippet();

	@Test
	public void basicResponse() throws IOException {
		this.snippet.expectHttpResponse("basic-response").withContents(httpResponse(OK));
		new HttpResponseSnippet().document("basic-response", result());
	}

	@Test
	public void nonOkResponse() throws IOException {
		this.snippet.expectHttpResponse("non-ok-response").withContents(
				httpResponse(BAD_REQUEST));

		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setStatus(BAD_REQUEST.value());
		new HttpResponseSnippet().document("non-ok-response",
				result(response));
	}

	@Test
	public void responseWithHeaders() throws IOException {
		this.snippet.expectHttpResponse("response-with-headers").withContents(
				httpResponse(OK) //
						.header("Content-Type", "application/json") //
						.header("a", "alpha"));

		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setHeader("a", "alpha");
		new HttpResponseSnippet().document("response-with-headers",
				result(response));
	}

	@Test
	public void responseWithContent() throws IOException {
		this.snippet.expectHttpResponse("response-with-content").withContents(
				httpResponse(OK).content("content"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().append("content");
		new HttpResponseSnippet().document("response-with-content",
				result(response));
	}

	@Test
	public void responseWithCustomSnippetAttributes() throws IOException {
		this.snippet.expectHttpResponse("response-with-snippet-attributes").withContents(
				containsString("Title for the response"));
		MockHttpServletRequest request = new MockHttpServletRequest();
		TemplateResourceResolver resolver = mock(TemplateResourceResolver.class);
		when(resolver.resolveTemplateResource("http-response"))
				.thenReturn(
						new FileSystemResource(
								"src/test/resources/custom-snippet-templates/http-response-with-title.snippet"));
		request.setAttribute(TemplateEngine.class.getName(), new MustacheTemplateEngine(
				resolver));
		new HttpResponseSnippet(attributes(key("title").value(
				"Title for the response"))).document("response-with-snippet-attributes",
				result(request));
	}

}
