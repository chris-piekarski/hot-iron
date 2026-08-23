package hotiron.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny JSON parse/print for MCP JSON-RPC. Not a general-purpose library.
 */
final class McpJson
{
	static Map<String, Object> parseObject(String json)
	{
		Object v = new Parser(json).parseValue();
		if (!(v instanceof Map))
			throw new IllegalArgumentException("expected object");
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) v;
		return map;
	}

	static String getString(Map<String, Object> o, String key)
	{
		Object v = o == null ? null : o.get(key);
		return v == null ? null : String.valueOf(v);
	}

	static Object get(Map<String, Object> o, String key)
	{
		return o == null ? null : o.get(key);
	}

	@SuppressWarnings("unchecked")
	static Map<String, Object> getObject(Map<String, Object> o, String key)
	{
		Object v = get(o, key);
		if (v instanceof Map)
			return (Map<String, Object>) v;
		return null;
	}

	static Integer getInt(Map<String, Object> o, String key)
	{
		Object v = get(o, key);
		if (v instanceof Number)
			return Integer.valueOf(((Number) v).intValue());
		if (v instanceof String)
		{
			try
			{
				return Integer.valueOf(Integer.parseInt((String) v));
			}
			catch (NumberFormatException e)
			{
				return null;
			}
		}
		return null;
	}

	static Boolean getBoolean(Map<String, Object> o, String key)
	{
		Object v = get(o, key);
		if (v instanceof Boolean)
			return (Boolean) v;
		if (v instanceof String)
		{
			if ("true".equalsIgnoreCase((String) v))
				return Boolean.TRUE;
			if ("false".equalsIgnoreCase((String) v))
				return Boolean.FALSE;
		}
		return null;
	}

	static Double getDouble(Map<String, Object> o, String key)
	{
		Object v = get(o, key);
		if (v instanceof Number)
			return Double.valueOf(((Number) v).doubleValue());
		if (v instanceof String)
		{
			try
			{
				return Double.valueOf(Double.parseDouble((String) v));
			}
			catch (NumberFormatException e)
			{
				return null;
			}
		}
		return null;
	}

	static String rpcResult(Object id, String resultJson)
	{
		return "{\"jsonrpc\":\"2.0\",\"id\":" + idLiteral(id) + ",\"result\":" + resultJson + "}";
	}

	static String rpcError(Object id, int code, String message)
	{
		return "{\"jsonrpc\":\"2.0\",\"id\":" + idLiteral(id) + ",\"error\":{\"code\":" + code + ",\"message\":"
				+ SpectrumSnapshot.Json.quote(message) + "}}";
	}

	static String idLiteral(Object id)
	{
		if (id == null)
			return "null";
		if (id instanceof Number)
			return String.valueOf(id);
		return SpectrumSnapshot.Json.quote(String.valueOf(id));
	}

	private static final class Parser
	{
		private final String s;
		private int i;

		Parser(String s)
		{
			this.s = s == null ? "" : s;
		}

		Object parseValue()
		{
			skip();
			if (i >= s.length())
				throw new IllegalArgumentException("empty");
			char c = s.charAt(i);
			if (c == '{')
				return parseObj();
			if (c == '[')
				return parseArr();
			if (c == '"')
				return parseStr();
			if (c == 't' || c == 'f' || c == 'n')
				return parseWord();
			return parseNum();
		}

		private Map<String, Object> parseObj()
		{
			i++;
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			skip();
			if (peek('}'))
			{
				i++;
				return m;
			}
			while (true)
			{
				skip();
				String k = parseStr();
				skip();
				if (!peek(':'))
					throw new IllegalArgumentException("expected :");
				i++;
				Object v = parseValue();
				m.put(k, v);
				skip();
				if (peek('}'))
				{
					i++;
					return m;
				}
				if (!peek(','))
					throw new IllegalArgumentException("expected ,");
				i++;
			}
		}

		private List<Object> parseArr()
		{
			i++;
			List<Object> a = new ArrayList<Object>();
			skip();
			if (peek(']'))
			{
				i++;
				return a;
			}
			while (true)
			{
				a.add(parseValue());
				skip();
				if (peek(']'))
				{
					i++;
					return a;
				}
				if (!peek(','))
					throw new IllegalArgumentException("expected ,");
				i++;
			}
		}

		private String parseStr()
		{
			if (!peek('"'))
				throw new IllegalArgumentException("expected string");
			i++;
			StringBuilder b = new StringBuilder();
			while (i < s.length())
			{
				char c = s.charAt(i++);
				if (c == '"')
					return b.toString();
				if (c == '\\' && i < s.length())
				{
					char e = s.charAt(i++);
					if (e == 'n')
						b.append('\n');
					else if (e == 'r')
						b.append('\r');
					else if (e == 't')
						b.append('\t');
					else
						b.append(e);
				}
				else
					b.append(c);
			}
			throw new IllegalArgumentException("unterminated string");
		}

		private Object parseWord()
		{
			if (s.startsWith("true", i))
			{
				i += 4;
				return Boolean.TRUE;
			}
			if (s.startsWith("false", i))
			{
				i += 5;
				return Boolean.FALSE;
			}
			if (s.startsWith("null", i))
			{
				i += 4;
				return null;
			}
			throw new IllegalArgumentException("bad token at " + i);
		}

		private Number parseNum()
		{
			int start = i;
			if (peek('-'))
				i++;
			while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.' || s.charAt(i) == 'e'
					|| s.charAt(i) == 'E' || s.charAt(i) == '+' || s.charAt(i) == '-'))
				i++;
			String t = s.substring(start, i);
			if (t.indexOf('.') >= 0 || t.indexOf('e') >= 0 || t.indexOf('E') >= 0)
				return Double.valueOf(t);
			try
			{
				return Long.valueOf(t);
			}
			catch (NumberFormatException e)
			{
				return Double.valueOf(t);
			}
		}

		private void skip()
		{
			while (i < s.length() && Character.isWhitespace(s.charAt(i)))
				i++;
		}

		private boolean peek(char c)
		{
			return i < s.length() && s.charAt(i) == c;
		}
	}
}
