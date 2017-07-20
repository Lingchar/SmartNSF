package org.openntf.xrest.designer.codeassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.openntf.xrest.designer.dsl.MethodContainer;

public class VEProposal extends AbstractProposalFactory implements CodeProposal {
	final ProposalParameter parameter;

	public VEProposal(ProposalParameter pp) {
		super(pp.getImageRegistry());
		this.parameter = pp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openntf.xrest.designer.codeassist.CodeProposal#suggestions(int)
	 */
	@Override
	public List<ICompletionProposal> suggestions(int offset) {
		VariableExpression expression = (VariableExpression) parameter.getNode();
		String variableName = expression.getName();
		CodeContext context = this.parameter.getCodeContext();
		if (context.getDeclaredVariables().containsKey(variableName)) {
			Class<?> cl = context.getDeclaredVariables().get(variableName);
			return buildListFromClass(cl, offset);
		} else {
			System.out.println("CANT FIND: "+ variableName);
			MethodCallExpression me = findCurrentMethodContext();
			if (me != null) {
				VariableExpression recivier = (VariableExpression) me.getReceiver();
				System.out.println("RTEXT "+recivier.getText());
				System.out.println(me.getText());
				Class<?> cl = parameter.getRegistry().searchMethodClass(recivier.getName(), me.getMethodAsString());
				if (parameter.getRegistry().isMethodConditioned(cl, expression.getName())) {
					List<MethodContainer> mc = parameter.getRegistry().getMethodContainers(cl, expression.getName());
					return buildConditionedMethodContainerProposals(mc, offset);
				}
			}
		}
		return Collections.emptyList();
	}

	private List<ICompletionProposal> buildConditionedMethodContainerProposals(List<MethodContainer> mc, int offset) {
		List<ICompletionProposal> cps = new ArrayList<ICompletionProposal>();
		for (MethodContainer container : mc) {
			String value = buildNameFromMethodContainer(container);
			String info = buildInfoFromMethodContainer(container);
			CompletionProposal cp = new CompletionProposal(value, offset, 0, value.length(), parameter.getImageRegistry().get("bullet_green.png"), info, null, null);
			cps.add(cp);
		}
		return cps;
	}

	private String buildInfoFromMethodContainer(MethodContainer container) {
		StringBuilder sb = new StringBuilder(" "+container.getCondition());
		sb.append(" (");
		sb.append(container.getClosureClass().getSimpleName());
		sb.append(")");
		return sb.toString();
	}

	private String buildNameFromMethodContainer(MethodContainer container) {
		StringBuilder sb = new StringBuilder(container.getCondition());
		sb.append(" {");
		boolean hasParam = false;
		if (container.getClosureParameters() != null) {
			for (Class<?> cl : container.getClosureParameters()) {
				if (!hasParam) {
					hasParam = true;
				} else {
					sb.append(", ");
				}
				sb.append(cl.getSimpleName());
			}
			if (hasParam) {
				sb.append(" -> ");
			}
		}
		sb.append("\n}");

		return sb.toString();
	}

	private MethodCallExpression findCurrentMethodContext() {
		List<ASTNode> hierRevers = new ArrayList<ASTNode>(parameter.getHierarchie());
		Collections.reverse(hierRevers);
		MethodCallExpression me = null;
		for (ASTNode node : hierRevers) {
			System.out.println(node.getClass().getSimpleName() +" -> "+node.getText());
			if (node instanceof MethodCallExpression) {
				me = (MethodCallExpression) node;
				//break;
				System.out.println("------> FOUND ME");
			}
		}
		return me;
	}

}
