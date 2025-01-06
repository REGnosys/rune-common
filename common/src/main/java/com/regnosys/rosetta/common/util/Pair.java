package com.regnosys.rosetta.common.util;

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

import java.util.Objects;

public class Pair<L, R> {

	private final L left;
	private final R right;

	private Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public static <L, R> Pair<L, R> of(L left, R right) {
		return new Pair<>(left, right);
	}

	public L left() {
		return left;
	}

	public R right() {
		return right;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Pair)) {
			return false;
		}
		Pair<?, ?> otherP = (Pair<?, ?>) other;
		return Objects.equals(left, otherP.left) && Objects.equals(right, otherP.right);
	}

	@Override
	public int hashCode() {
		return Objects.hash(left, right);
	}

	@Override
	public String toString() {
		return "(" + Objects.toString(left, "null") + "," + Objects.toString(right, "null") + ")";
	}
}
