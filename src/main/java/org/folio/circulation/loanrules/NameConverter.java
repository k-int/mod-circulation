package org.folio.circulation.loanrules;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.folio.circulation.loanrules.LoanRulesParser.CriteriumContext;
import org.folio.circulation.loanrules.LoanRulesParser.PolicyContext;

import java.util.List;
import java.util.Map;

/**
 * Make a UUID to name or a name to UUID conversion of the names in each criterium
 * and the policy names in the loan rules file.
 */
public class NameConverter extends LoanRulesBaseListener {
  /*
   * Implementation idea:
   * Terence Parr: "The Definitive ANTLR 4 Reference", Pragmatic Bookshelf, 2012,
   * section "Accessing Hidden Channels" pages 206-208.
   */

  private TokenStreamRewriter tokenStreamRewriter;
  /** maps the name type (one of t, a, b, c, s, m, g, policy) to a map where
   * the key String maps to its replacement String. For example
   * replacements.get("t").get("rare") = "12ffffff-2222-4b5e-a7bd-064b8d177231"
   */
  private Map<String, Map<String,String>> replacements;

  /**
   * Create a conversion for the token stream using the specified name replacement.
   *
   * @param tokens  the TokenStream to convert
   * @param replacements  maps the name type (one of t, a, b, c, s, m, g, policy) to a map where
   *                      the key String maps to its replacement String. For example
   *                      replacements.get("t").get("rare") = "12ffffff-2222-4b5e-a7bd-064b8d177231"
   */
  public NameConverter(BufferedTokenStream tokens, Map<String, Map<String,String>> replacements) {
    this.replacements = replacements;
    tokenStreamRewriter = new TokenStreamRewriter(tokens);
  }

  /**
   * The rewritten tokens.
   * @return the name conversion result
   */
  public String getText() {
    return tokenStreamRewriter.getText();
  }

  /**
   * Replace each name with the value from replacementMap.
   * @param names  list of names to replace
   * @param replacementMap  maps the old name to the new name
   */
  private void replace(List<TerminalNode> names, Map<String, String> replacementMap) {
    if (replacementMap == null) {
      return;
    }

    for (TerminalNode name : names) {
      String newName = replacementMap.get(name.getText());
      if (newName != null) {
        tokenStreamRewriter.replace(name.getSymbol(), newName);
      }
    }
  }

  @Override
  public void exitCriterium(CriteriumContext criteriumContext) {
    Token start = criteriumContext.getStart();
    String criterionType = start.getText();  // one of t, a, b, c, s, m, g
    replace(criteriumContext.NAME(), replacements.get(criterionType));
  }

  @Override
  public void exitPolicy(PolicyContext policyContext) {
    replace(policyContext.NAME(), replacements.get("policy"));
  }
}
