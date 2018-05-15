package org.intellij.plugins.postcss.highlighting;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.css.impl.CssElementTypes;
import com.intellij.psi.css.impl.util.CssHighlighter;
import com.intellij.psi.css.impl.util.scheme.CssElementDescriptorFactory2;
import com.intellij.psi.tree.IElementType;
import org.intellij.plugins.postcss.PostCssElementTypes;
import org.intellij.plugins.postcss.lexer.PostCssHighlightingLexer;
import org.intellij.plugins.postcss.lexer.PostCssTokenTypes;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class PostCssSyntaxHighlighter extends CssHighlighter {
  private static final Map<IElementType, TextAttributesKey> mapping;

  @NonNls
  private static final String KEYWORD_ID = "POST_CSS_KEYWORD";

  public static final TextAttributesKey KEYWORD = createTextAttributesKey(KEYWORD_ID, DefaultLanguageHighlighterColors.KEYWORD);
  public static final TextAttributesKey IDENTIFIER = createTextAttributesKey("POST_CSS_IDENT", CssHighlighter.CSS_IDENT);
  public static final TextAttributesKey STRING = createTextAttributesKey("POST_CSS_STRING", CssHighlighter.CSS_STRING);
  public static final TextAttributesKey IMPORTANT = createTextAttributesKey("POST_CSS_IMPORTANT", CssHighlighter.CSS_IMPORTANT);
  public static final TextAttributesKey PROPERTY_NAME = createTextAttributesKey("POST_CSS_PROPERTY_NAME", CssHighlighter.CSS_PROPERTY_NAME);
  public static final TextAttributesKey PROPERTY_VALUE = createTextAttributesKey("POST_CSS_PROPERTY_VALUE", CssHighlighter.CSS_PROPERTY_VALUE);
  public static final TextAttributesKey FUNCTION = createTextAttributesKey("POST_CSS_FUNCTION", CssHighlighter.CSS_FUNCTION);
  public static final TextAttributesKey URL = createTextAttributesKey("POST_CSS_URL", CssHighlighter.CSS_URL);
  public static final TextAttributesKey NUMBER = createTextAttributesKey("POST_CSS_NUMBER", CssHighlighter.CSS_NUMBER);
  public static final TextAttributesKey COLOR = createTextAttributesKey("POST_CSS_COLOR", CssHighlighter.CSS_COLOR);
  public static final TextAttributesKey PSEUDO = createTextAttributesKey("POST_CSS_PSEUDO", CssHighlighter.CSS_PSEUDO);
  public static final TextAttributesKey COMMENT = createTextAttributesKey("POST_CSS_COMMENT", CssHighlighter.CSS_COMMENT);
  public static final TextAttributesKey TAG_NAME = createTextAttributesKey("POST_CSS_TAG_NAME", CssHighlighter.CSS_TAG_NAME);
  public static final TextAttributesKey ID_SELECTOR = createTextAttributesKey("POST_CSS_ID_SELECTOR", CssHighlighter.CSS_ID_SELECTOR);
  public static final TextAttributesKey UNICODE_RANGE = createTextAttributesKey("POST_CSS_UNICODE_RANGE", CssHighlighter.CSS_UNICODE_RANGE);
  public static final TextAttributesKey PARENTHESES = createTextAttributesKey("POST_CSS_PARENTHESES", CssHighlighter.CSS_PARENTHESES);
  public static final TextAttributesKey BRACKETS = createTextAttributesKey("POST_CSS_BRACKETS", CssHighlighter.CSS_BRACKETS);
  public static final TextAttributesKey BRACES = createTextAttributesKey("POST_CSS_BRACES", CssHighlighter.CSS_BRACES);
  public static final TextAttributesKey DOT = createTextAttributesKey("POST_CSS_DOT", CssHighlighter.CSS_DOT);
  public static final TextAttributesKey SEMICOLON = createTextAttributesKey("POST_CSS_SEMICOLON", CssHighlighter.CSS_SEMICOLON);
  public static final TextAttributesKey COLON = createTextAttributesKey("POST_CSS_COLON", CssHighlighter.CSS_COLON);
  public static final TextAttributesKey COMMA = createTextAttributesKey("POST_CSS_COMMA", CssHighlighter.CSS_COMMA);
  public static final TextAttributesKey BAD_CHARACTER = createTextAttributesKey("POST_CSS_BAD_CHARACTER", CssHighlighter.CSS_BAD_CHARACTER);

  static {
    mapping = new HashMap<IElementType, TextAttributesKey>();
    mapping.put(CssElementTypes.CSS_COMMENT, COMMENT);
    mapping.put(CssElementTypes.CSS_HASH, ID_SELECTOR);
    mapping.put(CssElementTypes.CSS_IDENT, IDENTIFIER);
    mapping.put(CssElementTypes.CSS_STRING_TOKEN, STRING);
    mapping.put(CssElementTypes.CSS_NUMBER, NUMBER);
    mapping.put(CssElementTypes.CSS_IMPORTANT, IMPORTANT);
    mapping.put(CssElementTypes.CSS_PROPERTY_NAME, PROPERTY_NAME);
    mapping.put(CssElementTypes.CSS_PROPERTY_VALUE, PROPERTY_VALUE);
    mapping.put(CssElementTypes.CSS_TAG_NAME, TAG_NAME);
    mapping.put(CssElementTypes.CSS_FUNCTION_TOKEN, FUNCTION);
    mapping.put(CssElementTypes.CSS_URI_START, FUNCTION);
    mapping.put(CssElementTypes.CSS_URL, URL);
    mapping.put(CssElementTypes.CSS_COLOR, COLOR);
    mapping.put(CssElementTypes.CSS_PSEUDO, PSEUDO);
    mapping.put(CssElementTypes.CSS_UNICODE_RANGE, UNICODE_RANGE);
    mapping.put(CssElementTypes.CSS_LPAREN, PARENTHESES);
    mapping.put(CssElementTypes.CSS_RPAREN, PARENTHESES);
    mapping.put(CssElementTypes.CSS_LBRACE, BRACES);
    mapping.put(CssElementTypes.CSS_RBRACE, BRACES);
    mapping.put(CssElementTypes.CSS_LBRACKET, BRACKETS);
    mapping.put(CssElementTypes.CSS_RBRACKET, BRACKETS);
    mapping.put(CssElementTypes.CSS_PERIOD, DOT);
    mapping.put(CssElementTypes.CSS_COLON, COLON);
    mapping.put(CssElementTypes.CSS_COMMA, COMMA);
    mapping.put(CssElementTypes.CSS_SEMICOLON, SEMICOLON);
    mapping.put(PostCssTokenTypes.AMPERSAND, IDENTIFIER);
    mapping.put(CssElementTypes.CSS_KEYWORD, KEYWORD);
    mapping.put(PostCssElementTypes.POST_CSS_NEST_SYM, KEYWORD);
    mapping.put(CssElementTypes.CSS_BAD_CHARACTER, BAD_CHARACTER);
  }

  @NotNull
  @Override
  public Lexer getHighlightingLexer() {
    return new PostCssHighlightingLexer(CssElementDescriptorFactory2.getInstance().getValueIdentifiers());
  }

  @NotNull
  @Override
  public TextAttributesKey[] getTokenHighlights(final IElementType tokenType) {
    if (mapping.containsKey(tokenType)) {
      return pack(mapping.get(tokenType));
    }
    return super.getTokenHighlights(tokenType);
  }
}