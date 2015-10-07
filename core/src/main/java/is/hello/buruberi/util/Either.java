/*
 * Copyright 2015 Hello, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package is.hello.buruberi.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import rx.functions.Action1;
import rx.functions.Func1;

/**
 * <code>type Either 'l 'r = Left 'l | Right 'r</code>
 * <p/>
 * A pseudo-discriminated union which represents one of two possible values.
 * @param <Left>    The left value type. A successful value by convention.
 * @param <Right>   The right value type. An error value by convention.
 */
public final class Either<Left, Right> {
    private final boolean isLeft;
    private final @Nullable Left left;
    private final @Nullable Right right;


    //region Creation

    public static <Left, Right> Either<Left, Right> left(@NonNull Left value) {
        return new Either<>(true, value, null);
    }

    public static <Left, Right> Either<Left, Right> right(@NonNull Right value) {
        return new Either<>(false, null, value);
    }

    private Either(boolean isLeft, @Nullable Left left, @Nullable Right right) {
        this.isLeft = isLeft;
        this.left = left;
        this.right = right;
    }

    //endregion


    //region Introspection

    public boolean isLeft() {
        return isLeft;
    }

    public Left getLeft() {
        if (!isLeft()) {
            throw new NullPointerException();
        }

        return left;
    }

    public Right getRight() {
        if (isLeft()) {
            throw new NullPointerException();
        }

        return right;
    }

    public void match(@NonNull Action1<Left> onLeft,
                      @NonNull Action1<Right> onRight) {
        if (isLeft()) {
            onLeft.call(getLeft());
        } else {
            onRight.call(getRight());
        }
    }

    public <R> R map(@NonNull Func1<Left, R> onLeft,
                     @NonNull Func1<Right, R> onRight) {
        if (isLeft()) {
            return onLeft.call(getLeft());
        } else {
            return onRight.call(getRight());
        }
    }

    //endregion


    //region Identity

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Either either = (Either) o;

        return !(left != null ? !left.equals(either.left) : either.left != null) &&
               !(right != null ? !right.equals(either.right) : either.right != null);
    }

    @Override
    public int hashCode() {
        if (isLeft()) {
            return left != null ? left.hashCode() : 0;
        } else {
            return right != null ? right.hashCode() : 0;
        }
    }

    @Override
    public String toString() {
        if (isLeft()) {
            return "{Either left=" + left + "}";
        } else {
            return "{Either right=" + right + "}";
        }
    }

    //endregion
}
