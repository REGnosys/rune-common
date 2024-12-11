package com.regnosys.rosetta.common.translation.flat;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
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
 * ==============
 */

import org.junit.jupiter.api.Test;

import static com.regnosys.rosetta.common.translation.flat.IndexCapturePath.IndexCapturePathElement;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexCapturePathTest {

	@Test
	void test() {
		assertEquals("activity", IndexCapturePathElement.parse("activity").toString());
		assertEquals("activity[1]", IndexCapturePathElement.parse("activity[1]").toString());
		assertEquals("activity[activityNum]",IndexCapturePathElement.parse("activity[activityNum]").toString());
		assertEquals("activity[1]",IndexCapturePathElement.parse("activity(1)").toString());
		assertEquals("activity[activityNum]",IndexCapturePathElement.parse("activity(activityNum)").toString());
	}

}
