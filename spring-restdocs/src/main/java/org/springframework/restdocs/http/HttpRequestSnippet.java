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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.snippet.DocumentableHttpServletRequest;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.restdocs.snippet.TemplatedSnippet;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * A {@link Snippet} that documents an HTTP request.
 *
 * @author Andy Wilkinson
 */
class HttpRequestSnippet extends TemplatedSnippet {

	private static final String MULTIPART_BOUNDARY = "6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm";

	HttpRequestSnippet() {
		this(null);
	}

	HttpRequestSnippet(Map<String, Object> attributes) {
		super("http-request", attributes);
	}

	@Override
	public Map<String, Object> document(MvcResult result) throws IOException {
		DocumentableHttpServletRequest request = new DocumentableHttpServletRequest(
				result.getRequest());
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("method", result.getRequest().getMethod());
		model.put("path", request.getRequestUriWithQueryString());
		model.put("headers", getHeaders(request));
		model.put("requestBody", getRequestBody(request));
		return model;
	}

	private List<Map<String, String>> getHeaders(DocumentableHttpServletRequest request) {
		List<Map<String, String>> headers = new ArrayList<>();
		if (requiresHostHeader(request)) {
			headers.add(header(HttpHeaders.HOST, request.getHost()));
		}

		for (Entry<String, List<String>> header : request.getHeaders().entrySet()) {
			for (String value : header.getValue()) {
				if (header.getKey() == HttpHeaders.CONTENT_TYPE
						&& request.isMultipartRequest()) {
					headers.add(header(header.getKey(),
							String.format("%s; boundary=%s", value, MULTIPART_BOUNDARY)));
				}
				else {
					headers.add(header(header.getKey(), value));
				}

			}
		}
		if (requiresFormEncodingContentType(request)) {
			headers.add(header(HttpHeaders.CONTENT_TYPE,
					MediaType.APPLICATION_FORM_URLENCODED_VALUE));
		}
		return headers;
	}

	private String getRequestBody(DocumentableHttpServletRequest request)
			throws IOException {
		StringWriter httpRequest = new StringWriter();
		PrintWriter writer = new PrintWriter(httpRequest);
		if (request.getContentLength() > 0) {
			writer.println();
			writer.print(request.getContentAsString());
		}
		else if (request.isPostRequest() || request.isPutRequest()) {
			if (request.isMultipartRequest()) {
				writeParts(request, writer);
			}
			else {
				String queryString = request.getParameterMapAsQueryString();
				if (StringUtils.hasText(queryString)) {
					writer.println();
					writer.print(queryString);
				}
			}
		}
		return httpRequest.toString();
	}

	private void writeParts(DocumentableHttpServletRequest request, PrintWriter writer)
			throws IOException {
		writer.println();
		for (Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
			for (String value : parameter.getValue()) {
				writePartBoundary(writer);
				writePart(parameter.getKey(), value, null, writer);
				writer.println();
			}
		}
		for (Entry<String, List<MultipartFile>> entry : request.getMultipartFiles()
				.entrySet()) {
			for (MultipartFile file : entry.getValue()) {
				writePartBoundary(writer);
				writePart(file, writer);
				writer.println();
			}
		}
		writeMultipartEnd(writer);
	}

	private void writePartBoundary(PrintWriter writer) {
		writer.printf("--%s%n", MULTIPART_BOUNDARY);
	}

	private void writePart(String name, String value, String contentType,
			PrintWriter writer) {
		writer.printf("Content-Disposition: form-data; name=%s%n", name);
		if (StringUtils.hasText(contentType)) {
			writer.printf("Content-Type: %s%n", contentType);
		}
		writer.println();
		writer.print(value);
	}

	private void writePart(MultipartFile part, PrintWriter writer) throws IOException {
		writePart(part.getName(), new String(part.getBytes()), part.getContentType(),
				writer);
	}

	private void writeMultipartEnd(PrintWriter writer) {
		writer.printf("--%s--", MULTIPART_BOUNDARY);
	}

	private boolean requiresHostHeader(DocumentableHttpServletRequest request) {
		return request.getHeaders().get(HttpHeaders.HOST) == null;
	}

	private boolean requiresFormEncodingContentType(DocumentableHttpServletRequest request) {
		return request.getHeaders().getContentType() == null
				&& (request.isPostRequest() || request.isPutRequest())
				&& StringUtils.hasText(request.getParameterMapAsQueryString());
	}

	private Map<String, String> header(String name, String value) {
		Map<String, String> header = new HashMap<>();
		header.put("name", name);
		header.put("value", value);
		return header;
	}
}