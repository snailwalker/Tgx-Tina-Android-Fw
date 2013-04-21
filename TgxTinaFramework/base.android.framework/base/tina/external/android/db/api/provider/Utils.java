 /*******************************************************************************
  * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *******************************************************************************/
 package base.tina.external.android.db.api.provider;

import java.util.Set;

import android.content.ContentValues;
import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.CursorWrapper;

public class Utils
{

	public static final void copyLong(String key, ContentValues from, ContentValues to) {
		Long i = from.getAsLong(key);
		if (i != null) to.put(key, i);
	}

	public static final void copyInteger(String key, ContentValues from, ContentValues to) {
		Integer i = from.getAsInteger(key);
		if (i != null) to.put(key, i);
	}

	public static final void copyBoolean(String key, ContentValues from, ContentValues to) {
		Boolean b = from.getAsBoolean(key);
		if (b != null) to.put(key, b);
	}

	public static void validateSelection(String selection, Set<String> allowedColumns) {
		try
		{
			if (selection == null) { return; }
			Lexer lexer = new Lexer(selection, allowedColumns);
			parseExpression(lexer);
			if (lexer.currentToken() != Lexer.TOKEN_END) { throw new IllegalArgumentException("syntax error"); }
		}
		catch (RuntimeException ex)
		{
			throw ex;
		}

	}

	// expression <- ( expression ) | statement [AND_OR ( expression ) |
	// statement] *
	// | statement [AND_OR expression]*
	private static void parseExpression(Lexer lexer) {
		for (;;)
		{
			// ( expression )
			if (lexer.currentToken() == Lexer.TOKEN_OPEN_PAREN)
			{
				lexer.advance();
				parseExpression(lexer);
				if (lexer.currentToken() != Lexer.TOKEN_CLOSE_PAREN) { throw new IllegalArgumentException("syntax error, unmatched parenthese"); }
				lexer.advance();
			}
			else
			{
				// statement
				parseStatement(lexer);
			}
			if (lexer.currentToken() != Lexer.TOKEN_AND_OR) break;
			lexer.advance();
		}
	}

	// statement <- COLUMN COMPARE VALUE
	// | COLUMN IS NULL
	private static void parseStatement(Lexer lexer) {
		// both possibilities start with COLUMN
		if (lexer.currentToken() != Lexer.TOKEN_COLUMN) { throw new IllegalArgumentException("syntax error, expected column name"); }
		lexer.advance();

		// statement <- COLUMN COMPARE VALUE
		if (lexer.currentToken() == Lexer.TOKEN_COMPARE)
		{
			lexer.advance();
			if (lexer.currentToken() != Lexer.TOKEN_VALUE) { throw new IllegalArgumentException("syntax error, expected quoted string"); }
			lexer.advance();
			return;
		}

		// statement <- COLUMN IS NULL
		if (lexer.currentToken() == Lexer.TOKEN_IS)
		{
			lexer.advance();
			if (lexer.currentToken() != Lexer.TOKEN_NULL) { throw new IllegalArgumentException("syntax error, expected NULL"); }
			lexer.advance();
			return;
		}

		// didn't get anything good after COLUMN
		throw new IllegalArgumentException("syntax error after column name");
	}

	public static final void copyString(String key, ContentValues from, ContentValues to) {
		String s = from.getAsString(key);
		if (s != null) to.put(key, s);
	}

	public static class ReadOnlyCursorWrapper
			extends
			CursorWrapper
			implements
			CrossProcessCursor
	{
		public ReadOnlyCursorWrapper(Cursor cursor)
		{
			super(cursor);
			mCursor = (CrossProcessCursor) cursor;
		}

		public boolean deleteRow() {
			throw new SecurityException("cursors are read-only");
		}

		public boolean commitUpdates() {
			throw new SecurityException("cursors are read-only");
		}

		public void fillWindow(int pos, CursorWindow window) {
			mCursor.fillWindow(pos, window);
		}

		public CursorWindow getWindow() {
			return mCursor.getWindow();
		}

		public boolean onMove(int oldPosition, int newPosition) {
			return mCursor.onMove(oldPosition, newPosition);
		}

		private CrossProcessCursor	mCursor;
	}

	/**
	 * A simple lexer that recognizes the words of our restricted subset of SQL
	 * where clauses
	 */
	private static class Lexer
	{
		public static final int		TOKEN_START			= 0;
		public static final int		TOKEN_OPEN_PAREN	= 1;
		public static final int		TOKEN_CLOSE_PAREN	= 2;
		public static final int		TOKEN_AND_OR		= 3;
		public static final int		TOKEN_COLUMN		= 4;
		public static final int		TOKEN_COMPARE		= 5;
		public static final int		TOKEN_VALUE			= 6;
		public static final int		TOKEN_IS			= 7;
		public static final int		TOKEN_NULL			= 8;
		public static final int		TOKEN_END			= 9;

		private final String		mSelection;
		private final Set<String>	mAllowedColumns;
		private int					mOffset				= 0;
		private int					mCurrentToken		= TOKEN_START;
		private final char[]		mChars;

		public Lexer(String selection, Set<String> allowedColumns)
		{
			mSelection = selection;
			mAllowedColumns = allowedColumns;
			mChars = new char[mSelection.length()];
			mSelection.getChars(0, mChars.length, mChars, 0);
			advance();
		}

		public int currentToken() {
			return mCurrentToken;
		}

		public void advance() {
			char[] chars = mChars;

			// consume whitespace
			while (mOffset < chars.length && chars[mOffset] == ' ')
			{
				++mOffset;
			}

			// end of input
			if (mOffset == chars.length)
			{
				mCurrentToken = TOKEN_END;
				return;
			}

			// "("
			if (chars[mOffset] == '(')
			{
				++mOffset;
				mCurrentToken = TOKEN_OPEN_PAREN;
				return;
			}

			// ")"
			if (chars[mOffset] == ')')
			{
				++mOffset;
				mCurrentToken = TOKEN_CLOSE_PAREN;
				return;
			}

			// "?"
			if (chars[mOffset] == '?')
			{
				++mOffset;
				mCurrentToken = TOKEN_VALUE;
				return;
			}

			// "=" and "=="
			if (chars[mOffset] == '=')
			{
				++mOffset;
				mCurrentToken = TOKEN_COMPARE;
				if (mOffset < chars.length && chars[mOffset] == '=')
				{
					++mOffset;
				}
				return;
			}

			// ">" and ">="
			if (chars[mOffset] == '>')
			{
				++mOffset;
				mCurrentToken = TOKEN_COMPARE;
				if (mOffset < chars.length && chars[mOffset] == '=')
				{
					++mOffset;
				}
				return;
			}

			// "<", "<=" and "<>"
			if (chars[mOffset] == '<')
			{
				++mOffset;
				mCurrentToken = TOKEN_COMPARE;
				if (mOffset < chars.length && (chars[mOffset] == '=' || chars[mOffset] == '>'))
				{
					++mOffset;
				}
				return;
			}

			// "!="
			if (chars[mOffset] == '!')
			{
				++mOffset;
				mCurrentToken = TOKEN_COMPARE;
				if (mOffset < chars.length && chars[mOffset] == '=')
				{
					++mOffset;
					return;
				}
				throw new IllegalArgumentException("Unexpected character after !");
			}

			// columns and keywords
			// first look for anything that looks like an identifier or a
			// keyword
			// and then recognize the individual words.
			// no attempt is made at discarding sequences of underscores with no
			// alphanumeric
			// characters, even though it's not clear that they'd be legal
			// column names.
			if (isIdentifierStart(chars[mOffset]))
			{
				int startOffset = mOffset;
				++mOffset;
				while (mOffset < chars.length && isIdentifierChar(chars[mOffset]))
				{
					++mOffset;
				}
				String word = mSelection.substring(startOffset, mOffset);
				if (mOffset - startOffset <= 4)
				{
					if (word.equals("IS"))
					{
						mCurrentToken = TOKEN_IS;
						return;
					}
					if (word.equals("OR") || word.equals("AND"))
					{
						mCurrentToken = TOKEN_AND_OR;
						return;
					}
					if (word.equals("NULL"))
					{
						mCurrentToken = TOKEN_NULL;
						return;
					}
				}
				if (mAllowedColumns.contains(word))
				{
					mCurrentToken = TOKEN_COLUMN;
					return;
				}
				throw new IllegalArgumentException("unrecognized column or keyword");
			}

			// quoted strings
			if (chars[mOffset] == '\'')
			{
				++mOffset;
				while (mOffset < chars.length)
				{
					if (chars[mOffset] == '\'')
					{
						if (mOffset + 1 < chars.length && chars[mOffset + 1] == '\'')
						{
							++mOffset;
						}
						else
						{
							break;
						}
					}
					++mOffset;
				}
				if (mOffset == chars.length) { throw new IllegalArgumentException("unterminated string"); }
				++mOffset;
				mCurrentToken = TOKEN_VALUE;
				return;
			}

			// anything we don't recognize
			throw new IllegalArgumentException("illegal character");
		}

		private static final boolean isIdentifierStart(char c) {
			return c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
		}

		private static final boolean isIdentifierChar(char c) {
			return c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
		}
	}
}
