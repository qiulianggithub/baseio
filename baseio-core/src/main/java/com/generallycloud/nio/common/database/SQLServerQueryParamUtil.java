/*
 * Copyright 2015 GenerallyCloud.com
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
package com.generallycloud.nio.common.database;

import java.sql.SQLException;

import com.generallycloud.nio.common.ArrayUtil;

public class SQLServerQueryParamUtil implements QueryParamUtil{

	@Override
	public Object[] page(int firstResult, int pageSize, Object[] params)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] top(int size, Object[] params) throws SQLException {
		if (params == null) {
			return new Object[] { size };
		}
		return ArrayUtil.groupArray(params, new Object[] { size });
	}
}
