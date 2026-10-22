PASTA_SRC    = src
PASTA_BIN    = bin
ARQUIVOS_SRC = $(wildcard $(PASTA_SRC)/*.java)
FLAGS        = -Xlint:all

# $(info ARQUIVOS_SRC = $(ARQUIVOS_SRC))

all: $(PASTA_BIN)
	javac $(FLAGS) -d $(PASTA_BIN) $(ARQUIVOS_SRC)

$(PASTA_BIN):
	mkdir -p $(PASTA_BIN)

Servidor: all
	java -cp $(PASTA_BIN) Servidor $(ARGS)

Cliente: all
	java -cp $(PASTA_BIN) Cliente $(ARGS)

clean:
	rm -rf $(PASTA_BIN)

