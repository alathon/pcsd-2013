/**
 * 
 */
package com.acertainbookstore.server;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreMessageTag;
import com.acertainbookstore.utils.BookStoreResponse;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * BookStoreHTTPMessageHandler implements the message handler class which is invoked to handle messages received by the BookStoreHTTPServerUtility. It decodes the HTTP message and
 * invokes the CertainBookStore server API
 */
public class BookStoreHTTPMessageHandler extends AbstractHandler {

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		BookStoreMessageTag messageTag;
		String requestURI;

		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		requestURI = request.getRequestURI();

		// Need to do request multi-plexing
		if (!BookStoreUtility.isEmpty(requestURI) && requestURI.toLowerCase().startsWith("/stock")) {
			messageTag = BookStoreUtility.convertURItoMessageTag(requestURI.substring(6)); // the request is from store
			// manager, more 
			// sophisticated security
			// features could be added
			// here
		} else {
			messageTag = BookStoreUtility.convertURItoMessageTag(requestURI);
		}
		// the RequestURI before the switch
		if (messageTag == null) {
			System.out.println("Unknown message tag");
		} else {
			BookStoreResponse resp = null;
			String xml = BookStoreUtility.extractPOSTDataFromRequest(request);
			
			switch (messageTag) {
				case CLEAR:
					resp = BookStoreResponseCrafter.clear(CertainBookStore.getInstance());
					break;
				case ADDBOOKS:
					resp = BookStoreResponseCrafter.addBooks(CertainBookStore.getInstance(),xml);
					break;

				case ADDCOPIES:
					resp = BookStoreResponseCrafter.addCopies(CertainBookStore.getInstance(),xml);
					break;
					
				case LISTBOOKS:
					resp = BookStoreResponseCrafter.listBooks(CertainBookStore.getInstance());
					break;
					
				case UPDATEEDITORPICKS:
					resp = BookStoreResponseCrafter.updateEditorPicks(CertainBookStore.getInstance(),xml);
					break;

				case BUYBOOKS:
					resp = BookStoreResponseCrafter.buyBooks(CertainBookStore.getInstance(),xml);
					break;

				case GETBOOKS:
					resp = BookStoreResponseCrafter.getBooks(CertainBookStore.getInstance(),xml);
					break;

				case EDITORPICKS:
					String numBooksString = URLDecoder.decode(request.getParameter(BookStoreConstants.BOOK_NUM_PARAM), "UTF-8");
					resp = BookStoreResponseCrafter.editorPicks(CertainBookStore.getInstance(),numBooksString);
					break;

				case RATEBOOKS:
					resp = BookStoreResponseCrafter.rateBooks(CertainBookStore.getInstance(),xml);
					break;
					
				case GETTOPRATED:
					numBooksString = URLDecoder.decode(request.getParameter(BookStoreConstants.BOOK_NUM_PARAM), "UTF-8");
					resp = BookStoreResponseCrafter.getTopRated(CertainBookStore.getInstance(),numBooksString);
					break;
					
				case GETINDEMAND:
					resp = BookStoreResponseCrafter.getInDemand(CertainBookStore.getInstance());
					break;
					
				default:
					System.out.println("Unhandled message tag");
					break;
			}
			String xmlResp = BookStoreUtility.serializeObjectToXMLString(resp);
			response.getWriter().println(xmlResp);
		}
		// Mark the request as handled so that the HTTP response can be sent
		baseRequest.setHandled(true);
	}
}