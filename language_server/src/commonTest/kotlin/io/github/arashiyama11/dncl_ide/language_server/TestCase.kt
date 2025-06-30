package io.github.arashiyama11.dncl_ide.language_server


object TestCase {
    const val exam2025_0 = """Akibi = [5, 3, 4]
buinsu = 3
tantou = 1
buin を 2 から buinsu まで 1 ずつ増やしながら繰り返す:
  もし Akibi[buin] < Akibi[tantou] ならば:
    tantou = buin
表示する("次の工芸品の担当は部員", tantou, "です")"""
    const val exam2025_1 = """Nissu = [4, 1, 3, 1, 3, 4, 2, 4, 3]
kougeihinsu = 9
Akibi = [1, 1, 1]
buinsu = 3
kougeihin を 1 から kougeihinsu まで 1 ずつ増やしながら繰り返す:
  tantou = 1
  buin を 2 から buinsu まで 1 ずつ増やしながら繰り返す:
    もし Akibi[buin] < Akibi[tantou] ならば:
      tantou = buin
  表示する("工芸品", kougeihin, " … ",
                "部員", tantou, "：",
                Akibi[tantou], "日目～",
                Akibi[tantou] + Nissu[kougeihin] - 1, "日目")
  Akibi[tantou] = Akibi[tantou] + Nissu[kougeihin]"""

    const val Sisaku2022 =
        """Angoubun = ["p", "y", "e", "b", " ", "c", "m", "y", "b", "o", " ", "k", "x", "n", " ", "c", "o", "f", "o", "x", " ", "i", "o", "k", "b", "c", " ", "k", "q", "y", " "]
Hindo = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]
i を 0 から 要素数(Angoubun)-1 まで 1 ずつ増やしながら:
  bangou = 差分( Angoubun[i] )
  もし bangou != -1 ならば:
    Hindo[bangou] = Hindo[bangou] + 1
表示する(Hindo)
"""

    const val Sisaku2022_0 =
        """Kouka = [1,5,10,50,100]
kingaku = 46
maisu = 0, nokori = kingaku
i を 4 から 0 まで 1 ずつ減らしながら繰り返す：
  maisu = maisu + nokori ÷ Kouka[i]
  nokori = nokori % Kouka[i]
表示する(maisu)"""

    const val Sisaku2022_1 =
        """kakaku = 46
min_maisu = 100
tsuri を 0 から 99 まで 1 ずつ増やしながら繰り返す：
  shiharai = kakaku + tsuri
  maisu = 枚数(shiharai) + 枚数(tsuri)
  もし maisu < min_maisu ならば：
    min_maisu = maisu
表示する(min_maisu)"""

    const val Sisaku2022_2 = """Data = [3,18,29,33,48,52,62,77,89,97]
kazu = 要素数(Data)
表示する("0～99の数字を入力してください")
atai = 整数変換(【外部からの入力】)
hidari = 0 , migi = kazu - 1
owari = 0
hidari <= migi and owari == 0 の間繰り返す:
  aida = (hidari+migi) ÷ 2 # 演算子÷は商の整数値を返す
  もし Data[aida] == atai ならば:
    表示する(atai, "は", aida, "番目にありました")
    owari = 1
  そうでなくもし Data[aida] < atai ならば:
    hidari = aida + 1
  そうでなければ:
    migi = aida - 1
もし owari == 0 ならば:
  表示する(atai, "は見つかりませんでした")
表示する("添字", " ", "要素")
i を 0 から kazu - 1 まで 1 ずつ増やしながら繰り返す:
  表示する(i, " ", Data[i])"""

    val all = listOf(exam2025_0, exam2025_1, Sisaku2022, Sisaku2022_0, Sisaku2022_1, Sisaku2022_2)

    const val MaisuFunction = """
関数 枚数(kingaku) を:
  Kouka = [1,5,10,50,100]
  maisu = 0, nokori = kingaku
  i を 4 から 0 まで 1 ずつ減らしながら繰り返す：
    maisu = maisu + nokori ÷ Kouka[i]
    nokori = nokori % Kouka[i]
  戻り値(maisu)
と定義する
"""

    const val lifeGame = """
dirs = [[0,1],[0,-1],[-1,-1],[-1,0],[-1,1],[1,1],[1,0],[1,-1]]
関数 生存セル数(board,x,y)を:
  result = 0
  i を 0 から 7 まで 1 ずつ増やしながら繰り返す:
    nx = x + dirs[i][1]
    ny = y + dirs[i][0]
    もし !(ny < 0 or ny >= 要素数(board) または nx < 0 or nx >= 要素数(board[0])) ならば:
      result = result + board[ny][nx]
  戻り値(result)
と定義する
関数 進化(board)を:
  result = []
  i を 0 から 要素数(board) - 1 まで 1 ずつ増やしながら繰り返す:
    row = []
    j を 0 から 要素数(board[0]) - 1 まで 1 ずつ増やしながら繰り返す:
      cell = board[i][j]
      count = 生存セル数(board,j,i)
      cr = 0
      もし cell == 1 ならば:
          もし count == 2 or count == 3 ならば:
            cr = 1
      そうでなくもし count==3 ならば:
        cr = 1
      末尾追加(row,cr)
    末尾追加(result,row)
  戻り値(result)
と定義する
関数 セルを表示(board)を:
  res = ""
  i を 0 から 要素数(board) - 1 まで 1 ずつ増やしながら繰り返す:
    line = ""
    j を 0 から 要素数(board[0]) - 1 まで 1 ずつ増やしながら繰り返す:
      もし board[i][j] == 1 ならば:
        line =line + "⬜️"
      そうでなければ:
        line =line + "⬛️"
    res = res + line + "\n"
  表示する(res)
と定義する
関数 ランダムボード(width,height)を:
  board = []
  i を 0 から height-1 まで 1 ずつ増やしながら繰り返す:
    row = []
    j を 0 から width-1 まで 1 ずつ増やしながら繰り返す:
      r = 乱数(0,1)
      もし r<0.5 ならば:
        末尾追加(row,0)
      そうでなければ:
        末尾追加(row,1)
    末尾追加(board,row)
  戻り値(board)
と定義する
関数 main()を:
  board = ランダムボード(40,20)
  i を 0 から 3000 まで 1 ずつ増やしながら繰り返す:
     出力消去()
     セルを表示(board)
     待機(100)
     board = 進化(board)
と定義する

main()
"""
}