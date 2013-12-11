import sys

rules = {
	'<PROGRAM>' : [
		['<IDENT>', '<ENV>', '<DATA>', '<PROC>']
	],
	'<VAR_LIST>': [
		['<VAR_DECL>', '<VAR_LIST>']
	],
	'<VAR_DECL>': [
		['INTEGER', 'IDENTIFIER', 'PIC', 'IMAGE', '<VAR_DECL_TAIL>']
	],
	'<VAR_DECL_TAIL>': [
		['END_OF_INSTRUCTION'],
		['VALUE', 'INTEGER', 'END_OF_INSTRUCTION']
	],
	'<VAR_LIST>': [
		['<VAR_DECL>', '<VAR_LIST>'],
		[]
	]
}

first = {
	'<PROGRAM>' : ['IDENTIFICATION'],
	'<IDENT>' : ['IDENTIFICATION'],
	'<ENV>' : ['ENVIRONMENT'],
	'<DATA>' : ['DATA'],
	'<PROC>' : [],
	'<VAR_LIST>' : ['INTEGER', ''],
	'<VAR_DECL>' : ['INTEGER'],
	'<VAR_DECL_TAIL>' : ['VALUE', 'PROCEDURE', 'END_OF_INSTRUCTION']
}

follow = {
	'<PROGRAM>' : [],
	'<IDENT>' : ['ENVIRONMENT'],
	'<ENV>' : ['DATA'],
	'<DATA>' : ['PROCEDURE'],
	'<PROC>' : [],
	'<VAR_LIST>' : ['PROCEDURE'],
	'<VAR_DECL>' : ['INTEGER', 'PROCEDURE'],
	'<VAR_DECL_TAIL>' : ['INTEGER', 'PROCEDURE']
}

def line(identation = 0, text = ''):
	print('\t' * identation, text, sep = '')

def is_rule(element):
	return element[0] == '<'

def get_first(element):
	if is_rule(element): return first[element]
	else: return [element]

def rule_text(rule):
	return rule[1:-1]

def main():
	for unit in sorted(rules.keys()):
		line(0, 'public void handle_' + rule_text(unit) + '() throws Exception{')
		if len(rules[unit]) > 1:
			line(1, 'this.read();')
			line(1, 'switch(this.token.unit){')
			terminals = []
			for rule in rules[unit]:
				if len(rule) > 0:
					element = rule[0]
					for terminal in get_first(element):
						terminals.append('LexicalUnit.' + terminal)
						line(2, 'case ' + terminal + ':')
					if is_rule(element):
						line(3, 'this.unread();')
						line(3, 'this.handle_' + rule_text(element) + '();')
					for element in rule[1:]:
						if is_rule(element):
							line(3, 'this.handle_' + rule_text(element) + '();')
						else:
							line(3, 'this.read();')
							line(3, 'this.check_token_unit(LexicalUnit.' + element + ');')
					line(3, 'break;')

				elif len(follow[unit]) > 0:
					for terminal in follow[unit]:
						terminals.append('LexicalUnit.' + terminal)
						line(2, 'case ' + terminal + ':')
					line(3, 'this.unread();')
					line(3, 'break;')

			line(2, 'default:')
			line(3, 'this.handle_bad_token({' + ', '.join(terminals) + '});')
			line(3, 'break;')
			line(1, '}')

		else:
			for element in rules[unit][0]:
				if is_rule(element):
					line(1, 'this.handle_' + rule_text(element) + '();')
				else:
					line(1, 'this.read();')
					line(1, 'this.check_token_unit(LexicalUnit.' + element + ');')
		line(0, '}')
		line()


if __name__ == '__main__':
	main()