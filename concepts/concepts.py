#!/usr/bin/python

import requests
import json
from pprint import pprint

headers = {'content-type': 'application/json'}
json_file = open('json_file.json')
concepts = json.load(json_file)


for key in concepts.keys():
	answer_uuids = []
	question_uuid = ""
	question_answer_dict = concepts[key]
	for key1 in question_answer_dict.keys():
		if "question" in key1:
			r = requests.post(url, data=json.dumps(question_answer_dict[key1]), headers=headers, auth=('admin', 'Admin123'))
			for key2 in r.json().keys():
					if "uuid" in key2:
						question_uuid = r.json()[key2]
		elif "answer" in key1 : 
			answersList = question_answer_dict[key1]
			for answer in answersList:
				r = requests.post(url, data=json.dumps(answer), headers=headers, auth=('admin', 'Admin123'))
				for key2 in r.json().keys():
					if "uuid" in key2:
						answer_uuids.append(r.json()[key2])




	for subKey in question_answer_dict.keys():
		if "question" in subKey:
			url = url+question_uuid
			answerPost = {"answers": answer_uuids}
			r = requests.post(url, data=json.dumps(answerPost), headers=headers, auth=('admin', 'Admin123'))

