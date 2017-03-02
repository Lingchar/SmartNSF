package org.openntf.xrest.xsp.model.strategy;

import java.util.ArrayList;
import java.util.List;

import org.openntf.xrest.xsp.exec.Context;
import org.openntf.xrest.xsp.exec.DatabaseProvider;
import org.openntf.xrest.xsp.exec.ExecutorException;
import org.openntf.xrest.xsp.exec.convertor.DocumentListPaged2JsonConverter;
import org.openntf.xrest.xsp.exec.datacontainer.DocumentListPaginationDataContainer;
import org.openntf.xrest.xsp.model.DataContainer;
import org.openntf.xrest.xsp.model.RouteProcessor;

import com.ibm.commons.util.io.json.JsonObject;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewNavigator;

public class AllByViewPaged extends AbstractViewDatabaseStrategy implements
		StrategyModel<DocumentListPaginationDataContainer, JsonObject> {
	private Database dbAccess;
	private View viewAccess;
	private ViewNavigator vnav;
	private ViewEntry entCurrent;

	private static final int DEFAULT_START = 1;
	private static final int DEFAULT_COUNT = 10;

	@Override
	public DocumentListPaginationDataContainer buildDataContainer(final Context context) throws ExecutorException {
		try {
			dbAccess = DatabaseProvider.INSTANCE.getDatabase(getDatabaseNameValue(context), context.getDatabase(),
					context.getSession());
			viewAccess = dbAccess.getView(getViewNameValue(context));
			vnav = viewAccess.createViewNav();

			int start = getParamIntValue(context.getRequest().getParameter("start"), DEFAULT_START);
			int count = getParamIntValue(context.getRequest().getParameter("count"), DEFAULT_COUNT);

			List<Document> docs = new ArrayList<Document>();

			int skippedEntries = 0;
			// skip only when not starting at the beginning
			if (start > 1) {
				// skip counts from 0, so for start==2 we should skip(1)
				skippedEntries = vnav.skip(start - 1);
			}

			if (skippedEntries == start - 1) {
				entCurrent = vnav.getCurrent();
				int i = 0;
				while (entCurrent != null && entCurrent.isValid() && i < count) {
					docs.add(dbAccess.getDocumentByUNID(entCurrent.getUniversalID()));
					i++;
					ViewEntry nextEntry = vnav.getNext();
					// recycle!
					entCurrent.recycle();
					entCurrent = nextEntry;
				}
			} else {
				// TODO: handle the situation when we did not skipped to desired
				// start: is it because we are already at the end of view?
			}
			return new DocumentListPaginationDataContainer(docs, start, count, vnav.getCount());
		} catch (Exception ex) {
			throw new ExecutorException(500, ex, "", "getmodel");
		}
	}

	/**
	 * Convert given param to int, assing defaultVal when it is empty or less
	 * than 1
	 * 
	 * @param param
	 * @param defaultVal
	 * @return int value of param or defaulVal
	 */
	private int getParamIntValue(final String param, final int defaultVal) {
		int ret = defaultVal;
		if (null != param && !param.isEmpty()) {
			try {
				ret = Integer.parseInt(param);
				if (ret < 1) {
					ret = defaultVal;
				}
			} catch (NumberFormatException e) {
			}
		}
		return ret;
	}

	@Override
	public void cleanUp() {
		try {
			if (null != vnav) {
				vnav.recycle();
			}
			if (null != viewAccess) {
				viewAccess.recycle();
			}
			if (null != dbAccess) {
				dbAccess.recycle();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public JsonObject buildResponse(final Context context, final RouteProcessor routeProcessor,
			final DataContainer<?> dc) throws NotesException {
		DocumentListPaginationDataContainer docListDC = (DocumentListPaginationDataContainer) dc;
		DocumentListPaged2JsonConverter d2jc = new DocumentListPaged2JsonConverter(docListDC, routeProcessor, context);
		return d2jc.buildJsonFromDocument();
	}
}
